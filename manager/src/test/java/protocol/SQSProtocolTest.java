package protocol;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by hagai_lvi on 30/03/2016.
 */
public class SQSProtocolTest {

	@Test
	public void newTaskMessage() throws Exception {


		String bucketName = "mybucket";
		String key = "mykey";
		String message = SQSProtocol.newTaskMessage(bucketName, key);
		Assert.assertEquals("{" + "NEW_TASK" + "}" + "[" + bucketName + "," + key + "]", message);
	}

	@Test
	public void parse() throws Exception {
		//TODO need dependency injection
	}
}