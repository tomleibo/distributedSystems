package com.bgu.dsp.common.protocol.localtomanager;

public interface LocalToManagerCommand extends Runnable {

	/**
	 * @return true if the manager shouldn't listen to any more tasks
	 */
	boolean shouldTerminate();
}
