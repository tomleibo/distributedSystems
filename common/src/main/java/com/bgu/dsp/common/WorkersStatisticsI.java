package com.bgu.dsp.common;

import java.util.UUID;

/**
 * Created by hagai_lvi on 16/04/2016.
 */
public interface WorkersStatisticsI {
	void addSuccessfulTask(UUID workerUUID);

	void addFaultyTask(UUID workerUUID);
}
