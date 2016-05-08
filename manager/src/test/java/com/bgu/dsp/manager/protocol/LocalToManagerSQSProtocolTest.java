package com.bgu.dsp.manager.protocol;

import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerCommand;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerSQSProtocol;
import com.bgu.dsp.common.protocol.localtomanager.NewTaskCommand;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by hagai_lvi on 30/03/2016.
 */
public class LocalToManagerSQSProtocolTest {

	@Test
	public void newTaskMessage() throws Exception {

		String bucketName = "mybucket";
		String key = "mykey";

		String sqsName = "mySqs";
		float tasksPerWorker = 2.0F;
		String msg = LocalToManagerSQSProtocol.newTaskMessage(sqsName, bucketName, key, tasksPerWorker);

		LocalToManagerCommand task = LocalToManagerSQSProtocol.parse(msg);
		NewTaskCommand taskCasted = (NewTaskCommand) task;

		Assert.assertEquals(bucketName, taskCasted.getBucketName());
		Assert.assertEquals(key, taskCasted.getKey());
		Assert.assertEquals(sqsName, taskCasted.getSqsName());
		Assert.assertEquals(tasksPerWorker, taskCasted.getTasksPerWorker(),0);
		Assert.assertFalse(taskCasted.shouldTerminate());

	}

}