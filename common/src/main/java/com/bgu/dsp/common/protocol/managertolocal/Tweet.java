package com.bgu.dsp.common.protocol.managertolocal;

import java.util.List;

/**
 * Created by thinkPAD on 4/4/2016.
 */


public class Tweet {
    String tweet;
    List<String> entities;
    int sentiment;

    public Tweet(String tweet, List<String> entities, int sentiment) {
        this.tweet = tweet;
        this.entities = entities;
        this.sentiment = sentiment;
    }
}

