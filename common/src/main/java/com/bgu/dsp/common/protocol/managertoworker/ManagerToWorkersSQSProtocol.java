package com.bgu.dsp.common.protocol.managertoworker;

import com.bgu.dsp.common.protocol.MalformedMessageException;
import com.google.gson.Gson;

public class ManagerToWorkersSQSProtocol {


	/**
	 * Create a message that represents a new analyze task
	 * @return a message to send
	 */
	public static String newAnalyzeMessage(NewAnalyzeCommand command){

		return new Gson().toJson(command);
	}


	/**
	 * Parse a message received from the manager to the workers
	 * @throws MalformedMessageException if the message doesn't match the required pattern
	 */
	public static ManagerToWorkerCommand parse(String message) throws MalformedMessageException {
		return parseAnalyzeMessage(message);
	}

	private static ManagerToWorkerCommand parseAnalyzeMessage(String message) throws MalformedMessageException {
		return new Gson().fromJson(message, NewAnalyzeCommand.class);
	}
}
