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

    public Tweet() {

    }

    public Tweet setTweet(String tweet) {
        this.tweet = tweet;
        return this;
    }

    public Tweet setEntities(List<String> entities) {
        this.entities = entities;
        return this;
    }

    public Tweet setSentiment(int sentiment) {
        this.sentiment = sentiment;
        return this;
    }
}

