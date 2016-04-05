package com.bgu.dsp.common.protocol.localtomanager;

/**
 * Created by hagai_lvi on 30/03/2016.
 */
public interface LocalToManagerCommand extends Runnable {

	/**
	 * @return true if the manager shouldn't listen to any more tasks
	 */
	public boolean shouldTerminate();
}
