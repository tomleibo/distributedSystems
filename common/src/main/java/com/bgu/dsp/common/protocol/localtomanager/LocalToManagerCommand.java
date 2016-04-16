package com.bgu.dsp.common.protocol.localtomanager;

import com.bgu.dsp.common.WorkersStatisticsI;

public interface LocalToManagerCommand extends Runnable {

	void addWorkerStatisticsHandler(WorkersStatisticsI workersStatisticsI);


	int getTotalNumOfRequiredWorkers();

	/**
	 * @return true if the manager shouldn't listen to any more tasks
	 */
	boolean shouldTerminate();
}
