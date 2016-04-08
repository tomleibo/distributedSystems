package com.bgu.dsp.manager;

import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.awsUtils.Utils;
import com.bgu.dsp.common.protocol.MalformedMessageException;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerCommand;
import org.apache.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.bgu.dsp.awsUtils.Utils.LOCAL_TO_MANAGER_QUEUE_NAME;
import static com.bgu.dsp.awsUtils.Utils.MANAGER_TO_WORKERS_QUEUE_NAME;

public class Main {
	final static Logger logger = Logger.getLogger(Main.class);

	public static final int EXECUTOR_TIMEOUT = 60;

	public static void main(String[] args) {

		S3Utils.createBucket(Utils.MANAGER_TO_LOCAL_BUCKET_NAME);
		logger.warn("Manager created bucket " + Utils.MANAGER_TO_LOCAL_BUCKET_NAME + " and will not delete it.\n" +
				"This bucket is meant to save results and deliver them to the local application.");
		// This queue should be already created by the local
		String localToManagerQueueUrl = SQSUtils.getQueueUrlByName(LOCAL_TO_MANAGER_QUEUE_NAME);

		String managerToWorkersQueueUrl = SQSUtils.createQueue(MANAGER_TO_WORKERS_QUEUE_NAME);

		ExecutorService executor = Executors.newCachedThreadPool();

		SQSHandler sqsHandler = new SQSHandler();
		while (true){
			try {
				LocalToManagerCommand commandFromQueue = sqsHandler.getCommandFromQueue(localToManagerQueueUrl);
				if (commandFromQueue != null) {
					executor.execute(commandFromQueue);
					if (commandFromQueue.shouldTerminate()) {
						break;
					}
				}
			} catch (MalformedMessageException e){
				logger.error(e);
			}
		}

		logger.info("Shutting down executor, waiting for all tasks to be completed");
		waitForAllTasks(executor);

		logger.debug("Deleting " + MANAGER_TO_WORKERS_QUEUE_NAME + " queue");
		SQSUtils.deleteQueue(managerToWorkersQueueUrl);

		logger.info("All tasks completed, shutting down");

	}

	private static void waitForAllTasks(ExecutorService executor) {
		executor.shutdown();

		boolean keepWaiting = true;
		while (keepWaiting){
			try {
				keepWaiting = executor.awaitTermination(EXECUTOR_TIMEOUT, TimeUnit.SECONDS);
			} catch (InterruptedException e) {}
			logger.info("Executor didn't finish, waiting " + EXECUTOR_TIMEOUT + " seconds for it to finish");
		}
	}
}
