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
    private String error;

    public Tweet(String tweet, List<String> entities, int sentiment) {
        this.tweet = tweet;
        this.entities = entities;
        this.sentiment = sentiment;
    }

    public Tweet(String errorMessage){
        this.error = errorMessage;
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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tweet tweet1 = (Tweet) o;

        if (sentiment != tweet1.sentiment) return false;
        if (tweet != null ? !tweet.equals(tweet1.tweet) : tweet1.tweet != null) return false;
        if (entities != null ? !entities.equals(tweet1.entities) : tweet1.entities != null) return false;
        return !(getError() != null ? !getError().equals(tweet1.getError()) : tweet1.getError() != null);

    }

    @Override
    public int hashCode() {
        int result = tweet != null ? tweet.hashCode() : 0;
        result = 31 * result + (entities != null ? entities.hashCode() : 0);
        result = 31 * result + sentiment;
        result = 31 * result + (getError() != null ? getError().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Tweet{" +
                "tweet='" + tweet + '\'' +
                ", entities=" + entities +
                ", sentiment=" + sentiment +
                ", error='" + error + '\'' +
                '}';
    }
}

