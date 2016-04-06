package com.bgu.dsp.common.protocol.localtomanager;

import com.amazonaws.services.sqs.model.Message;
import com.bgu.dsp.awsUtils.EC2Utils;
import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.common.protocol.managertoworker.ManagerToWorkersSQSProtocol;
import com.bgu.dsp.common.protocol.workertomanager.WorkerToManagerMessage;
import com.bgu.dsp.common.protocol.workertomanager.WorkerToManagerSQSProtocol;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.HashMap;
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
	private final double linesPerWorker = 100.0;//TODO
	private final String sqsName;
	private final String bucketName;
	private final String key;
	private static int queueCounter = 0;
	private final boolean terminate;

	/**
	 *
	 * @param bucketName The bucket to which the local saved the tweets file
	 * @param key The key underwhich the local saved the tweets file.
	 * @param sqsName A queue that was created by the local, to which the manager
	 *                will send the reply (separate queue for each local)
	 * @param terminate if true, the manager will not accept any more tasks after this one, and will terminate
	 */
	public NewTaskCommand(String sqsName, String bucketName, String key, boolean terminate){
		this.sqsName = sqsName;
		this.bucketName = bucketName;
		this.key = key;
		this.terminate = terminate;
	}

	@Override
	public void run() {
		try {
			String fileContent = getFileContent();
			startWorkers(fileContent);
			String workersToManagerQueueName = createQueue();
			List<UUID> uuids = postTweetsToQueue(fileContent, workersToManagerQueueName);
			HashMap<UUID, String> replies = waitForAllReplies(workersToManagerQueueName, uuids);
			logger.debug("Got " + replies.size() + " replies.\n" +
					"Example: " + replies.values().iterator().next());// TODO remove this line and return actual value
		}
		catch (IOException e){
			// TODO tear down resources
			logger.fatal(
					MessageFormat.format("Command faild. sqsName={0}, bucketName={1}, key={2}", sqsName, bucketName, key),
					e);
		}
	}

	/**
	 * Wait for all the tasks sent to the workers to be completed
	 */
	private HashMap<UUID, String> waitForAllReplies(String workersToManagerQueueName, List<UUID> uuids) {
		String workersToManagerQueueUrl = SQSUtils.getQueueUrlByName(workersToManagerQueueName);
		HashMap<UUID, String> answers = new HashMap<>();
		while (answers.size() < uuids.size()){
			final int timeoutSeconds = 5;
			Message rawMessage =  SQSUtils.getMessage(workersToManagerQueueUrl, timeoutSeconds);
			if (rawMessage != null) {
				WorkerToManagerMessage msg = WorkerToManagerSQSProtocol.parse(rawMessage.getBody());
				answers.put(msg.getUuid(), msg.getMessage());
			}
			else {
				logger.debug("No message in the queue, waiting for " + timeoutSeconds + " more seconds.\n" +
						"Got " + answers.size() + "/" + uuids.size() + " messages.");
			}
		}
		return answers;
	}

	/**
	 * Post the file content to the queue for workers to consume.
	 * @param fileContent
	 * @param workersToManagerQueueName
	 * @return a list of UUIDs of the messages that were posted to the queue
	 * @throws IOException if failed to read the file
	 */
	private List<UUID> postTweetsToQueue(String fileContent, String workersToManagerQueueName) throws IOException {

		String managerToWorkersQueueUrl = SQSUtils.getQueueUrlByName(MANAGER_TO_WORKERS_QUEUE_NAME);

		BufferedReader bufReader = new BufferedReader(new StringReader(fileContent));

		List<UUID> uuids = new LinkedList<>();
		String line;

		while( (line = bufReader.readLine()) != null ) {
			UUID uuid = UUID.randomUUID();
			String msg = ManagerToWorkersSQSProtocol.newAnalyzeMessage(line, workersToManagerQueueName, uuid);

			SQSUtils.sendMessage(managerToWorkersQueueUrl, msg);
			uuids.add(uuid);
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

	/**
	 * Generate a random queue name
	 */
	private synchronized String getNewQueueName() {
		int id = queueCounter++;
		return "queue_" + id;
	}

	/**
	 * Calculate the required number of workers and start them
	 */
	private void startWorkers(String fileContent) {
		int numOfWorkers = getTotalNumOfRequiredWorkers(fileContent);
		int currentNumOfWorkers = EC2Utils.countWorkers();
		int workersToStart = numOfWorkers - currentNumOfWorkers;
		logger.info(currentNumOfWorkers + " workers are running\n" +
				"Starting " + workersToStart + " more workers for total of " + numOfWorkers + " workers.");
		// TODO start the workers
	}

	/**
	 * Calc the total number of workers that are required for this task
	 */
	private int getTotalNumOfRequiredWorkers(String fileContent) {
		int numberOfLines = countLines(fileContent);
		return (int)Math.ceil(numberOfLines / linesPerWorker);
	}

	private String getFileContent() throws IOException {
		// TODO what if the file is larger then the memory? can we save it in chunks?
		InputStream fileInputStream = S3Utils.getFileInputStream(bucketName, key);
		try {
			return IOUtils.toString(fileInputStream);
		} catch (IOException e) {
			throw new IOException("Failed to read file from S3", e);
		}
	}

	private int countLines(String fileContent) {
		Matcher m = Pattern.compile("\r\n|\r|\n").matcher(fileContent);
		int lines = 1;
		while (m.find()) {
			lines ++;
		}
		return lines;
	}

	@Override
	public boolean shouldTerminate() {
		return this.terminate;
	}
}
