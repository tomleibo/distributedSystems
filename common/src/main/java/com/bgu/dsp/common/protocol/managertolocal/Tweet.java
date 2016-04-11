package com.bgu.dsp.common.protocol.managertolocal;

import java.io.Serializable;
import java.util.List;

/**
 * Created by thinkPAD on 4/4/2016.
 */


public class Tweet implements Serializable{
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tweet tweet1 = (Tweet) o;

        if (sentiment != tweet1.sentiment) return false;
        if (!tweet.equals(tweet1.tweet)) return false;
        return entities.equals(tweet1.entities);

    }

    @Override
    public int hashCode() {
        int result = tweet.hashCode();
        result = 31 * result + entities.hashCode();
        result = 31 * result + sentiment;
        return result;
    }

    @Override
    public String toString() {
        return "Tweet{" +
                "tweet='" + tweet + '\'' +
                ", entities=" + entities +
                ", sentiment=" + sentiment +
                '}';
    }
}

