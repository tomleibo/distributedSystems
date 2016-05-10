package com.bgu.dsp.manager;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.model.Message;
import com.bgu.dsp.awsUtils.EC2Utils;
import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.awsUtils.Utils;
import com.bgu.dsp.common.MessageKeepAlive;
import com.bgu.dsp.common.protocol.MalformedMessageException;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerCommand;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerSQSProtocol;
import com.bgu.dsp.common.protocol.localtomanager.NewTaskCommand;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.bgu.dsp.awsUtils.Utils.*;

public class Main {
	final static Logger logger = Logger.getLogger(Main.class);

	private static WorkersStatistics workersStatistics = new WorkersStatistics();

	private static Integer expectedNumberOfWorkers = 0;

	public static int getExpectedNumberOfWorkers() {
		synchronized (expectedNumberOfWorkers) {
			return expectedNumberOfWorkers;
		}
	}

	public static void setExpectedNumberOfWorkers(int expectedNumberOfWorkers) {
		synchronized (Main.expectedNumberOfWorkers) {
			Main.expectedNumberOfWorkers = Math.max(expectedNumberOfWorkers, Main.expectedNumberOfWorkers);
		}
	}

	public static final int EXECUTOR_TIMEOUT = 60;

	public static void main(String[] args) {

		try {
			// The semaphore doesn't need to be fair because only one thread is using it
			Semaphore tasks = new Semaphore(Utils.NUM_OF_MANAGER_TASKS, false);

			try {
				S3Utils.createBucket(Utils.MANAGER_TO_LOCAL_BUCKET_NAME);
			} catch (AmazonClientException e) {
				logger.fatal("Could not create bucket for output. exiting", e);
				System.exit(1);
			}

			logger.warn("Manager created bucket " + Utils.MANAGER_TO_LOCAL_BUCKET_NAME + " and will not delete it.\n" +
					"This bucket is meant to save results and deliver them to the local application.");

			String localToManagerQueueUrl = getLocalToManagerQueueUrl();


			try {
				SQSUtils.createQueue(MANAGER_TO_WORKERS_QUEUE_NAME);
			} catch (AmazonClientException e) {
				logger.fatal("Could not create queue for output. exiting", e);
				System.exit(1);
			}

			Thread workersMonitor = new Thread(new WorkersMonitor());
			workersMonitor.start();

			ExecutorService executor = Executors.newCachedThreadPool();
			SQSHandler sqsHandler = new SQSHandler();
			String lastSqsName = null;

			while (true) {
				try {
					Message messageFromQueue = sqsHandler.getCommandFromQueue(localToManagerQueueUrl);

					if (messageFromQueue != null) {

						// Make sure that no one else is taking the message as long as this manager is working on it
						Thread messageKeepAlive = new Thread(new MessageKeepAlive(messageFromQueue, localToManagerQueueUrl, 30));
						messageKeepAlive.start();

						tasks.acquireUninterruptibly();

						LocalToManagerCommand commandFromQueue = LocalToManagerSQSProtocol.parse(messageFromQueue.getBody());

						if (commandFromQueue.shouldTerminate()) {
							if (commandFromQueue instanceof NewTaskCommand) {
								lastSqsName = ((NewTaskCommand) commandFromQueue).getSqsName();
							}
							messageKeepAlive.interrupt();
							SQSUtils.deleteMessage(localToManagerQueueUrl, messageFromQueue);
							tasks.release();
							break;
						} else {
							commandFromQueue.addWorkerStatisticsHandler(workersStatistics);
							setExpectedNumberOfWorkers(commandFromQueue.getTotalNumOfRequiredWorkers());

							executor.execute(
									() -> {
										commandFromQueue.run();
										messageKeepAlive.interrupt();
										SQSUtils.deleteMessage(localToManagerQueueUrl, messageFromQueue);
										tasks.release();
									});
						}
					}
				} catch (MalformedMessageException e) {
					logger.error(e);
				}
			}

			logger.info("Shutting down executor, waiting for all tasks to be completed");
			waitForAllTasks(executor);

			logger.info("Shutting down workers monitor");
			workersMonitor.interrupt();

			logger.info("Manager is now shutting down all the workers");
			EC2Utils.terminateAllWorkers();

			sendStatistics(lastSqsName);

			logger.info(workersStatistics.toString());
			logger.info("All tasks completed. Manager is exiting");
		} catch (Throwable t){
			logger.fatal(t);
		}
		finally {
			EC2Utils.terminateManager();
		}

	}

	private static String getLocalToManagerQueueUrl() {
		String localToManagerQueueUrl;
		while (true) {
			try {
				// This queue should be already created by the local
				localToManagerQueueUrl = SQSUtils.getQueueUrlByName(LOCAL_TO_MANAGER_QUEUE_NAME);
				break;
			} catch (Exception e) {
				logger.warn(
						"Could not find localToManager queue, available quesues:\n" +
								SQSUtils.listQueues() + "\n",
						e);
			}
		}
		return localToManagerQueueUrl;
	}

	private static void sendStatistics(String lastSqsName) {
		if (lastSqsName != null) {
			String fileKey = writeWorkersStatisticsToS3(workersStatistics.toString());
            try {
                if (fileKey != null) {
                    // filekey == null means failure to upload the stats file to s3
                    SQSUtils.sendMessage(
                            SQSUtils.getQueueUrlByName(lastSqsName),
                            fileKey
                    );
                } else {
                    SQSUtils.sendMessage(
                            SQSUtils.getQueueUrlByName(lastSqsName),
                            "NO_STATS_FILE"
                    );
                }
            }
            catch(AmazonClientException e) {
                logger.error("Exception thrown while sending statistics message to SQS queue.",e);
            }
		}
		else {
			logger.error("lastSqsName is null");
		}
	}

	private static String writeWorkersStatisticsToS3(String stats) {

		try {
			File statsFile = new File("stats");
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
			FileUtils.writeStringToFile(statsFile, stats);
			String fileKey = "statistics_" + timeStamp + ".txt";
			try {
                S3Utils.uploadFile(MANAGER_TO_LOCAL_BUCKET_NAME,
                        fileKey,
                        statsFile);
            }
            catch(AmazonClientException e) {
                logger.error("failed to upload file to S3",e);
            }
			return fileKey;

		} catch (Exception e) {
			logger.error("Failed to write workers statistics to file", e);
		}
		return null;
	}


	private static void waitForAllTasks(ExecutorService executor) {
		executor.shutdown();

		boolean keepWaiting = true;
		while (keepWaiting){
			try {
				keepWaiting = !executor.awaitTermination(EXECUTOR_TIMEOUT, TimeUnit.SECONDS);
			} catch (InterruptedException e) {}
			logger.info("Executor didn't finish, waiting " + EXECUTOR_TIMEOUT + " seconds for it to finish");
		}
		assert executor.isTerminated();
	}
}
