package com.bgu.dsp.common.protocol.localtomanager;

import com.bgu.dsp.common.protocol.MalformedMessageException;

/**
 * A protocol for the manager and local application to communicate over the SQS
 * Created by hagai_lvi on 30/03/2016.
 */
public class LocalToManagerSQSProtocol {

	private static final String NEW_TASK = "NEW_TASK";
	private static final String TERMINATE = "TERMINATE";

	/**
	 * Create a message that represents a new task
	 * @return a message to send
	 * @param bucketName The bucket to which the local saved the tweets file
	 * @param key The key underwhich the local saved the tweets file.
	 * @param SqsName A queue that was created by the local, to which the manager
	 *                will send the reply (seperate queue for each local)
	 */
	public static String newTaskMessage(String SqsName, String bucketName, String key){
		return "{" + NEW_TASK + "}[" + SqsName + "," + bucketName + "," + key + "]";
	}

	/**
	 * @return a message that represents a terminate message
	 */
	public static String newTerminateMessage() {
		return "{" + TERMINATE + "}[]";
	}


	/**
	 * Parses a message that was extracted from the queue and returns an executable command
	 * @param message the message received from the queue
	 * @return an executable {@link LocalToManagerCommand} or null if the command is TERMINATE
	 * @throws MalformedMessageException if the given string doesn't match the com.bgu.dsp.manager.protocol
	 */
	public static LocalToManagerCommand parse(String message) throws MalformedMessageException {
		String command = message.substring(message.indexOf("{") + 1, message.indexOf("}"));

		if (NEW_TASK.equals(command)){
			return parseNewTaskMessage(message);
		}
		else if (TERMINATE.equals(command)){
			return null;
		}

		throw new MalformedMessageException(message);
	}

	private static LocalToManagerCommand parseNewTaskMessage(String message) throws MalformedMessageException {
		String args = message.substring(message.indexOf("[") + 1, message.indexOf("]"));
		String[] argsArr = args.split(",");

		// expect bucketName and key arguments
		if (argsArr.length != 3){
			throw new MalformedMessageException(message);
		}

		String sqsName = argsArr[0];
		String bucketName = argsArr[1];
		String key = argsArr[2];

		return new NewTaskCommand(sqsName, bucketName, key);
	}
}
