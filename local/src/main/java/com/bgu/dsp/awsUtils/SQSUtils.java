package com.bgu.dsp.awsUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by thinkPAD on 3/29/2016.
 */
public class SQSUtils {

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
    }


}
