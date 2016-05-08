package com.bgu.dsp.common.protocol.localtomanager;

import com.bgu.dsp.common.protocol.MalformedMessageException;
import com.google.gson.Gson;

/**
 * A protocol for the manager and local application to communicate over the SQS
 * Created by hagai_lvi on 30/03/2016.
 */
public class LocalToManagerSQSProtocol {

	/**
	 * Create a message that represents a new task
	 * @return a message to send
	 * @param sqsName A queue that was created by the local, to which the manager
	 *                will send the reply (separate queue for each local)
	 * @param bucketName The bucket to which the local saved the tweets file
	 * @param key The key underwhich the local saved the tweets file.
	 * @param tasksPerWorker - Used to determine the workers/tasks ration. referred to as <i>"n"</i> in the assignment description
	 */
	public static String newTaskMessage(String sqsName, String bucketName, String key, float tasksPerWorker){
		NewTaskCommand newTaskCommand = new NewTaskCommand(sqsName, bucketName, key, tasksPerWorker);
		return  new Gson().toJson(newTaskCommand);
	}

	public static String newTerminateMessage(String sqsName) {
		return  new Gson().toJson(NewTaskCommand.getTerminateTask(sqsName));
	}


	/**
	 * Parses a message that was extracted from the queue and returns an executable command
	 * @param message the message received from the queue
	 * @return an executable {@link LocalToManagerCommand} or null if the command is TERMINATE
	 * @throws MalformedMessageException if the given string doesn't match the com.bgu.dsp.manager.protocol
	 */
	public static LocalToManagerCommand parse(String message) throws MalformedMessageException {
		return parseNewTaskMessage(message);
	}

	private static LocalToManagerCommand parseNewTaskMessage(String message) throws MalformedMessageException {
		return new Gson().fromJson(message, NewTaskCommand.class);
	}
}
