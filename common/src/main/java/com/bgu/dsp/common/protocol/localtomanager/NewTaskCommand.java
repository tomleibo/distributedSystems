package com.bgu.dsp.common.protocol.localtomanager;

import com.amazonaws.services.sqs.model.Message;
import com.bgu.dsp.awsUtils.EC2Utils;
import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.awsUtils.Utils;
import com.bgu.dsp.common.protocol.managertolocal.ManagerToLocalSqsProtocol;
import com.bgu.dsp.common.protocol.managertolocal.Tweet;
import com.bgu.dsp.common.protocol.managertolocal.serialize.TwitsWriter;
import com.bgu.dsp.common.protocol.managertoworker.ManagerToWorkersSQSProtocol;
import com.bgu.dsp.common.protocol.managertoworker.NewAnalyzeCommand;
import com.bgu.dsp.common.protocol.workertomanager.WorkerToManagerSQSProtocol;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.bgu.dsp.awsUtils.Utils.MANAGER_TO_WORKERS_QUEUE_NAME;

/**
 * Represents a command that is sent from the local to the manager about a new task
 */
public class NewTaskCommand implements LocalToManagerCommand {

	final static Logger logger = Logger.getLogger(NewTaskCommand.class);
	private final String sqsName;
	private final String bucketName;
	private final String key;
	private final boolean terminate;
	private final UUID taskID;
	private final float tasksPerWorker;

	public String getSqsName() {
		return sqsName;
	}

	public String getBucketName() {
		return bucketName;
	}

	public String getKey() {
		return key;
	}

	public boolean isTerminate() {
		return terminate;
	}

	public UUID getTaskID() {
		return taskID;
	}

	public float getTasksPerWorker() {
		return tasksPerWorker;
	}

	/**
	 * @param sqsName A queue that was created by the local, to which the manager
	 *                will send the reply (separate queue for each local)
	 * @param bucketName The bucket to which the local saved the tweets file
	 * @param key The key underwhich the local saved the tweets file.
	 * @param terminate if true, the manager will not accept any more tasks after this one, and will terminate
	 * @param tasksPerWorker - Used to determine the workers/tasks ration. referred to as <i>"n"</i> in the assignment description
	 */
	public NewTaskCommand(String sqsName, String bucketName, String key, boolean terminate, float tasksPerWorker){
		if (tasksPerWorker <= 0){
			throw new IllegalArgumentException("tasksPerWorker must be > 0, Got " + tasksPerWorker);
		}
		this.sqsName = sqsName;
		this.bucketName = bucketName;
		this.key = key;
		this.terminate = terminate;
		this.taskID = UUID.randomUUID();
		this.tasksPerWorker = tasksPerWorker;
	}

	@Override
	public void run() {
		String workersToManagerQueueName = null;
		try {
			startWorkers();
			workersToManagerQueueName = createQueue();
			List<UUID> uuids = postTweetsToQueue(workersToManagerQueueName);
			List<Tweet> replies = waitForAllReplies(workersToManagerQueueName, uuids);
			publishResultsToS3(replies);
			replyToLocal();
		}
		catch (IOException e){
			// TODO tear down resources
			logger.fatal(
					MessageFormat.format("Command faild. sqsName={0}, bucketName={1}, key={2}", sqsName, bucketName, key),
					e);
		}finally {
			if (workersToManagerQueueName != null){
				logger.info("Deleting workers to manager queue " + workersToManagerQueueName);
				String queueUrl = SQSUtils.getQueueUrlByName(workersToManagerQueueName);
				SQSUtils.deleteQueue(queueUrl);
			}

		}
	}

	private void replyToLocal() {
		String msg = ManagerToLocalSqsProtocol.newFileLocationMessage(Utils.MANAGER_TO_LOCAL_BUCKET_NAME, getResFilekey());
		String queueUrl = SQSUtils.getQueueUrlByName(sqsName);
		logger.debug("Manager sending reply to local in queue " + sqsName + "\nMessage is " + msg);
		SQSUtils.sendMessage(queueUrl, msg);
	}

	private String getResFilekey() {
		return "result_" + this.taskID.toString();
	}

	private void publishResultsToS3(List<Tweet> replies) throws IOException {

		String resFilekey = getResFilekey();
		TwitsWriter twitsWriter = new TwitsWriter(resFilekey);
		twitsWriter.init();

		for (Tweet reply : replies) {
			twitsWriter.write(reply);
		}
		twitsWriter.close();

		logger.info("Manager uploading results to S3. File-key " + resFilekey +
				", bucket " + Utils.MANAGER_TO_LOCAL_BUCKET_NAME);

		long start = System.currentTimeMillis();

		S3Utils.uploadFile(Utils.MANAGER_TO_LOCAL_BUCKET_NAME, resFilekey, new File(resFilekey));
		double time = (System.currentTimeMillis() - start) / 1000.0;
		logger.debug("Upload took " + time + " seconds" );

		File file = new File(resFilekey);
		if (! file.delete()){
			logger.warn("Manager could not delete results file after uploading it to S3.\n" +
					"File path is " + file.getAbsolutePath() );
		}
	}

	/**
	 * Wait for all the tasks sent to the workers to be completed
	 */
	private List<Tweet> waitForAllReplies(String workersToManagerQueueName, List<UUID> uuids) {
		String workersToManagerQueueUrl = SQSUtils.getQueueUrlByName(workersToManagerQueueName);
		List<Tweet> answers = new LinkedList<>();
		while (answers.size() < uuids.size()){
			final int timeoutSeconds = 20;
			Message rawMessage =  SQSUtils.getMessage(workersToManagerQueueUrl, timeoutSeconds);
			if (rawMessage != null) {
				logger.debug("Got message " + rawMessage);
				Tweet tweet = WorkerToManagerSQSProtocol.parse(rawMessage.getBody());
				answers.add(tweet);
			}
			else {

				logger.debug("No replies from workers in the queue, waiting for " + timeoutSeconds + " more seconds.\n" +
						"Got " + answers.size() + "/" + uuids.size() + " messages.");
			}
		}
		return answers;
	}

	/**
	 * Post the file content to the queue for workers to consume.
	 * @return a list of UUIDs of the messages that were posted to the queue
	 * @throws IOException if failed to read the file
	 */
	private List<UUID> postTweetsToQueue(String workersToManagerQueueName) throws IOException {

		String managerToWorkersQueueUrl = SQSUtils.getQueueUrlByName(MANAGER_TO_WORKERS_QUEUE_NAME);


		List<UUID> uuids = new LinkedList<>();
		String line;

		File file = S3Utils.downloadFile(bucketName, key);

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			while ((line = br.readLine()) != null) {
				UUID uuid = UUID.randomUUID();

				String msg = ManagerToWorkersSQSProtocol.newAnalyzeMessage(new NewAnalyzeCommand(uuid, workersToManagerQueueName, line));

				SQSUtils.sendMessage(managerToWorkersQueueUrl, msg);
				uuids.add(uuid);
			}
		}

		return uuids;
	}

	/**
	 * Create a new queue that will serve the manger and workers solely for this specific task
	 * @return the queue's name
	 */
	private String createQueue() {
		String workersToManagerQueueName = getNewQueueName();
		SQSUtils.createQueue(workersToManagerQueueName);
		return workersToManagerQueueName;
	}

	private String getNewQueueName() {
		return "task_queue_" + this.taskID.toString();
	}


	/**
	 * Calculate the required number of workers and start them
	 */
	private void startWorkers() {
		int numOfWorkers = getTotalNumOfRequiredWorkers();
		int currentNumOfWorkers = EC2Utils.countWorkers();
		int workersToStart = numOfWorkers - currentNumOfWorkers;
		if (workersToStart <= 0){
			logger.debug(currentNumOfWorkers + " are running. not starting any workers");
		}
		else {
			logger.info(currentNumOfWorkers + " workers are running\n" +
					"Starting " + workersToStart + " more workers for total of " + numOfWorkers + " workers.");
			EC2Utils.startWorkers(workersToStart);
		}

	}

	/**
	 * Calc the total number of workers that are required for this task
	 */
	@Override
	public int getTotalNumOfRequiredWorkers() {
		int numberOfLines = 0;
		try {
			numberOfLines = countLines();
		} catch (IOException e) {
			logger.error("Failed to read tweets file", e);
			return 0;
		}
		return (int)Math.ceil(numberOfLines / (double)tasksPerWorker);
	}

	private int countLines() throws IOException {
		File file = S3Utils.downloadFile(bucketName, key);
		int numberOfLines = 0;

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			while (br.readLine() != null) {
				numberOfLines++;
			}
		}
		return numberOfLines;
	}

	@Override
	public boolean shouldTerminate() {
		return this.terminate;
	}
}
