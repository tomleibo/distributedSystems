package protocol.localtomanager;

import protocol.MalformedMessageException;

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
	 */
	public static String newTaskMessage(String bucketName, String key){
		return "{" + NEW_TASK + "}[" + bucketName + "," + key + "]";
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
	 * @throws MalformedMessageException if the given string doesn't match the protocol
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
		if (argsArr.length != 2){
			throw new MalformedMessageException(message);
		}

		String bucketName = argsArr[0];
		String key = argsArr[1];

		return new NewTaskCommand(bucketName, key);
	}
}
