package com.bgu.dsp.manager;

import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.manager.protocol.localtomanager.LocalToManagerCommand;
import org.apache.log4j.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
	final static Logger logger = Logger.getLogger(Main.class);

	private static final String QUEUE_NAME = "local-to-manager-queue";
	public static final int EXECUTOR_TIMEOUT = 60;

	public static void main(String[] args) {

		String queueUrl = SQSUtils.createQueue(QUEUE_NAME);

		ExecutorService executor = Executors.newCachedThreadPool();

		SQSHandler sqsHandler = new SQSHandler();
		while (true){
			LocalToManagerCommand commandFromQueue = sqsHandler.getCommandFromQueue(queueUrl);
			if (commandFromQueue != null) {
				executor.execute(commandFromQueue);
			}else{
				// commandFromQueue != null means that we got a terminate request
				break;
			}
		}

		logger.info("Shuting down executor, waiting for all tasks to be completed");
		executor.shutdown();

		boolean keepWaiting = true;
		while (keepWaiting){
			try {
				keepWaiting = executor.awaitTermination(EXECUTOR_TIMEOUT, TimeUnit.SECONDS);
			} catch (InterruptedException e) {}
			logger.info("Executor didn't finish, waiting " + EXECUTOR_TIMEOUT + " seconds for it to finish");
		}
		logger.info("All tasks completed, shutting down");

	}
}
