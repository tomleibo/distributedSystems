package com.bgu.dsp.common.protocol.workertomanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Used to pass messages from the workers to the manager
 * Implemented using json serialization
 */
public class WorkerToManagerSQSProtocol {

	public static WorkerToManagerMessage parse(String json) {
		Gson gson = new GsonBuilder().create();
		return gson.fromJson(json, WorkerToManagerMessage.class);
	}

	/**
	 * Return a json that represents the given message
	 */
	public static String newCompletedMessage(WorkerToManagerMessage message){
		Gson gson = new GsonBuilder().create();
		return gson.toJson(message);
	}

	// TODO remove
	public static void main(String[] args) {
		List<WorkerToManagerMessage.Entity> entities = new LinkedList<>();
		entities.add(new WorkerToManagerMessage.Entity("eName", "eType"));
		WorkerToManagerMessage m = new WorkerToManagerMessage(UUID.randomUUID(), "hello", entities, 2);
		Gson gson = new GsonBuilder().create();

		String json = gson.toJson(m);

		System.out.println(json);

		System.out.println(parse(json));

	}

}

