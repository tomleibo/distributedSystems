package com.bgu.dsp.common.protocol.managertoworker;

import java.util.UUID;

/**
 * Created by hagai_lvi on 01/04/2016.
 */
public interface ManagerToWorkerCommand {
	void execute(TweetAnalyzer analyzer, UUID workerUUID);
}
