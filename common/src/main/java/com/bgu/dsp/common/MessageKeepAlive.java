package com.bgu.dsp.common;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.model.Message;
import com.bgu.dsp.awsUtils.SQSUtils;
import org.apache.log4j.Logger;

/**
 * This class extends the Visibility timeout of a message in a sqs queue
 */
public class MessageKeepAlive implements Runnable {

	final static Logger logger = Logger.getLogger(MessageKeepAlive.class);

	private Message message;
	private String queueUrl;
	private int timeoutSeconds;

	public MessageKeepAlive(Message message, String queueUrl, int timeoutSeconds) {
		this.message = message;
		this.queueUrl = queueUrl;
		this.timeoutSeconds = timeoutSeconds;
	}


	@Override
	public void run() {
		while (true) {
			try {
                SQSUtils.extendMessageVisibilityTimeout(this.message, this.queueUrl, this.timeoutSeconds + 20);
            }
            catch(AmazonClientException e) {
                logger.error("Exception thrown while extending msg visibility timeout.",e);
            }
			try {
				Thread.sleep(this.timeoutSeconds * 1000);
			} catch (InterruptedException e) {
				logger.debug(this.getClass().getSimpleName() + " got InterruptedException, exiting");
				break;
			}
		}

	}
}
