package com.bgu.dsp.manager;

import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.manager.protocol.localtomanager.LocalToManagerCommand;

/**
 * Created by hagai_lvi on 30/03/2016.
 */
public class Main {
	private static final String QUEUE_NAME = "local-to-manager-queue";

	public static void main(String[] args) {

		String queueUrl = SQSUtils.createQueue(QUEUE_NAME);


		SQSHandler sqsHandler = new SQSHandler();
		while (true){
			LocalToManagerCommand commandFromQueue = sqsHandler.getCommandFromQueue(queueUrl);
			if (commandFromQueue != null) {
				commandFromQueue.execute();
			}else{
				// commandFromQueue != null means that we got a terminate request
				return;
			}
		}
	}
}
