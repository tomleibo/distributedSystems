package com.bgu.dsp.common.protocol.managertoworker;

import com.bgu.dsp.common.protocol.managertolocal.Tweet;

/**
 * Created by hagai_lvi on 15/04/2016.
 */
public interface TweetAnalyzer {

	Tweet analyze(String tweet);
}
