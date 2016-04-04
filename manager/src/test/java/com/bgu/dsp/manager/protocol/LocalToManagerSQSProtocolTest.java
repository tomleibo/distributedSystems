package com.bgu.dsp.manager.protocol;

import org.junit.Assert;
import org.junit.Test;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerSQSProtocol;

/**
 * Created by hagai_lvi on 30/03/2016.
 */
public class LocalToManagerSQSProtocolTest {

	@Test
	public void newTaskMessage() throws Exception {


		String bucketName = "mybucket";
		String key = "mykey";
		String message = LocalToManagerSQSProtocol.newTaskMessage("" // TODO
				,bucketName, key);
		Assert.assertEquals("{" + "NEW_TASK" + "}" + "[" + bucketName + "," + key + "]", message);
	}

	@Test
	public void parse() throws Exception {
		//TODO need dependency injection
	}
}