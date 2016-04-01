import SQSCommands.SQSCommand;
import com.amazonaws.services.sqs.model.Message;
import com.bgu.dsp.awsUtils.SQSUtils;
import org.apache.log4j.Logger;
import protocol.MalformedMessageException;
import protocol.SQSProtocol;

/**
 * Handles all manager interaction with AWS SQS
 * Created by hagai_lvi on 30/03/2016.
 */
public class SQSHandler {
	private final static Logger logger = Logger.getLogger(SQSHandler.class);

	public SQSCommand getCommandFromQueue(String queueURL){
		Message message = SQSUtils.getMessage(queueURL);

		try {
			return SQSProtocol.parse(message.getBody());
		} catch (MalformedMessageException e) {
			logger.error("Malformed message recieved from the queue.\n" +
					"queueUrl = " + queueURL, e);
			throw new RuntimeException(e);
		}
	}
}
