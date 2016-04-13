package com.bgu.dsp.manager;

import com.amazonaws.services.sqs.model.Message;
import com.bgu.dsp.awsUtils.SQSUtils;
import org.apache.log4j.Logger;
import com.bgu.dsp.common.protocol.MalformedMessageException;

/**
 * Handles all manager interaction with AWS SQS
 * Created by hagai_lvi on 30/03/2016.
 */
public class SQSHandler {
	private final static Logger logger = Logger.getLogger(SQSHandler.class);

	public Message getCommandFromQueue(String queueURL) throws MalformedMessageException {
		Message message = SQSUtils.getMessage(queueURL, 20);

		if (message == null){
			return null;
		}
		return message;
	}
}
