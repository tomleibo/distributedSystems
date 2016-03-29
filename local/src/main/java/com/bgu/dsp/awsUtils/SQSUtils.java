package com.bgu.dsp.awsUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by thinkPAD on 3/29/2016.
 */
public class SQSUtils {
    final static Logger logger = Logger.getLogger(SQSUtils.class);

    private static AmazonSQS sqs;
    static {
        init();
    }


    public static void deleteQueue(String queueUrl) {
        sqs.deleteQueue(new DeleteQueueRequest(queueUrl));
    }

    public static void deleteMessage(String queueUrl, Message message) {
        String messageReceiptHandle = message.getReceiptHandle();
        sqs.deleteMessage(new DeleteMessageRequest(queueUrl, messageReceiptHandle));
    }

    public static List<Message> getMessages(String queueUrl) {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        return sqs.receiveMessage(receiveMessageRequest).getMessages();
    }

    public static boolean sendMessage(String queueUrl, String messageBody) {
        sqs.sendMessage(new SendMessageRequest(queueUrl, messageBody));
        //TODO md5 check
        return true;
    }

    public static ListQueuesResult getQueues() {
        return sqs.listQueues();
    }

    public static String createQueue(String queueName) {
        CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName);
        return sqs.createQueue(createQueueRequest).getQueueUrl();
    }

    public static void init() {
        AWSCredentials credentials = Utils.getAwsCredentials();
        sqs = new AmazonSQSClient(credentials);
        sqs.setRegion(Utils.region);
        if ("DEV".equals(System.getenv("DSP_MODE"))){
            String host = "localhost";
            int port = 4568;
            String URL = "http://" + host + ":" + port;
            logger.info("Using development SQS with url " + URL);
            sqs.setEndpoint(URL);
        }
    }


}
