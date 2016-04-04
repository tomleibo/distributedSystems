package com.bgu.dsp.manager.protocol.managertoworker;

import com.bgu.dsp.manager.protocol.MalformedMessageException;

import java.util.UUID;

public class ManagerToWorkersSQSProtocol {

	public static final String ANALYZE = "ANALYZE";


	/**
	 * Create a message that represents a new analyze task
	 * @return a message to send
	 */
	public static String newAnalyzeMessage(String tweetUrl, String sqsQueueName){

		UUID uuid = UUID.randomUUID();
		return "{" + ANALYZE + "}[" + uuid + "," + sqsQueueName + "," + tweetUrl+ "]";
	}


	/**
	 * Parse a message received from the manager to the workers
	 * @throws MalformedMessageException if the message doesn't match the required pattern
	 */
	public static ManagerToWorkerCommand parse(String message) throws MalformedMessageException {
		String command = message.substring(message.indexOf("{") + 1, message.indexOf("}"));

		if (ANALYZE.equals(command)) {
			return parseAnalyzeMessage(message);
		}

		throw new MalformedMessageException("Could not parse message " + message);
	}

	private static ManagerToWorkerCommand parseAnalyzeMessage(String message) throws MalformedMessageException {
		String args = message.substring(message.indexOf("[") + 1, message.indexOf("]"));
		String[] argsArr = args.split(",");
		final int EXPECTED_ARGS = 3;
		if (argsArr.length != EXPECTED_ARGS) {
			throw new MalformedMessageException(ANALYZE + " command requires exactly " + EXPECTED_ARGS +
					" arguments.\n" + "Original message is " + message);
		}

		UUID uuid;
		try {
			uuid = UUID.fromString(argsArr[0]);
		}
		catch (IllegalArgumentException e) {
			throw new MalformedMessageException(ANALYZE + " command 1st argument should be a UUID string.\n" +
					"Original message is " + message, e);
		}

		String sqsQueueName = argsArr[1];

		String tweetUrl = argsArr[2];

		return new NewAnalyzeCommand(uuid, sqsQueueName, tweetUrl);
	}
}
