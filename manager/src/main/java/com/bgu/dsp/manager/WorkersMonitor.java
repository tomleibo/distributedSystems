package com.bgu.dsp.manager;

import com.amazonaws.AbortedException;
import com.bgu.dsp.awsUtils.EC2Utils;
import org.apache.log4j.Logger;

/**
 * This class monitors that all the workers are running and starts new workers in case one of the workers fails
 */
public class WorkersMonitor implements Runnable{
	final static Logger logger = Logger.getLogger(WorkersMonitor.class);
	private static final int WAIT_FOR_WORKERS = 20;
	private static final int CYCLE_TIME = 20;

	@Override
	public void run() {
		String interuptedMessage = this.getClass().getSimpleName() + "Interrupted, Exiting";

		while (true) {
			int workers;
			try {
				workers = EC2Utils.countWorkers();
			}catch (AbortedException e){
				logger.info(interuptedMessage, e);
				break;
			}
			int expectedNumberOfWorkers = Main.getExpectedNumberOfWorkers();
			if (workers < expectedNumberOfWorkers) {
				logger.info("Expected " + expectedNumberOfWorkers + ", but found " + workers + " workers.\n" +
						"Waiting for " + WAIT_FOR_WORKERS + " more seconds for workers to start");
				try {
					Thread.sleep(WAIT_FOR_WORKERS * 1000);
				} catch (InterruptedException e) {
					logger.info(interuptedMessage);
					break;
				}
				try {
					workers = EC2Utils.countWorkers();
				}catch (AbortedException e){
					logger.info(interuptedMessage, e);
					break;
				}
				if (workers < expectedNumberOfWorkers) {
					int workersToStart = expectedNumberOfWorkers - workers;
					logger.warn("Missing workers, expected " + expectedNumberOfWorkers + " but got only " + workers + " workers.\n" +
							"Starting " + workersToStart + " More workers.");
					EC2Utils.startWorkers(workersToStart);
				}
			}

			try {
				Thread.sleep(1000 * CYCLE_TIME);
			} catch (InterruptedException e) {
				logger.info(interuptedMessage);
				break;
			}
		}
	}
}
