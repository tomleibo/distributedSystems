import SQSCommands.SQSCommand;

/**
 * Created by hagai_lvi on 30/03/2016.
 */
public class Main {
	private static final String QUEUE_URL = "";

	public static void main(String[] args) {
		SQSHandler sqsHandler = new SQSHandler();
		while (true){
			SQSCommand commandFromQueue = sqsHandler.getCommandFromQueue(QUEUE_URL);
			if (commandFromQueue != null) {
				commandFromQueue.execute();
			}else{
				// commandFromQueue != null means that we got a terminate request
				return;
			}
		}
	}
}
