package com.bgu.dsp.common.protocol.workertomanager;

import com.bgu.dsp.common.protocol.managertolocal.Tweet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Used to pass messages from the workers to the manager
 * Implemented using json serialization
 */
public class WorkerToManagerSQSProtocol {

	public static Tweet parse(String json) {
		Gson gson = new GsonBuilder().create();
		return gson.fromJson(json, Tweet.class);
	}

	/**
	 * Return a json that represents the given tweet <br>
	 * This message is to be written to the sqs queue by the worker
	 */
	public static String newCompletedMessage(Tweet message){
		Gson gson = new GsonBuilder().create();
		return gson.toJson(message);
	}


}

