package com.bgu.dsp.common.protocol.managertoworker;

import com.bgu.dsp.common.protocol.managertolocal.Tweet;

import java.util.UUID;

/**
 * Created by hagai_lvi on 15/04/2016.
 */
public interface TweetAnalyzer {

	Tweet analyze(String tweet, UUID workerUUID);
}
