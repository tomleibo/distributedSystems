package com.bgu.dsp.awsUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import org.apache.log4j.Logger;

import java.util.List;

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

	/**
	 * Return a single message if exists in the queue, otherwise return null
     */
    public static Message getMessage(String queueUrl) {
        return getMessage(queueUrl, 0);
    }
    public static Message getMessage(String queueUrl, int timeoutSeconds){
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl)
                .withMaxNumberOfMessages(1)
                .withWaitTimeSeconds(timeoutSeconds);

        List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();

        // We asked maxNumberOfMessages=1
        assert messages.size() <= 1;

        if (messages.size() > 0){
            return messages.get(0);
        }else{
            return null;
        }
    }

    public static boolean sendMessage(String queueUrl, String messageBody) {
        sqs.sendMessage(new SendMessageRequest(queueUrl, messageBody));
        //TODO md5 check
        return true;
    }

    public static ListQueuesResult getQueues() {
        return sqs.listQueues();
    }

    public static void extendMessageVisibilityTimeout(Message msg, String queueUrl, Integer newVisibilityTimeout) {
        ChangeMessageVisibilityRequest changeMessageVisibilityRequest = new ChangeMessageVisibilityRequest(
                queueUrl,
                msg.getReceiptHandle(),
                newVisibilityTimeout
        );
        sqs.changeMessageVisibility(changeMessageVisibilityRequest);
    }

    public static String createQueue(String queueName) {
        CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName);
        return sqs.createQueue(createQueueRequest).getQueueUrl();
    }

    public static String getQueueSize(String queueUrl){
        String key = "ApproximateNumberOfMessages";
        GetQueueAttributesResult approximateNumberOfMessages = sqs.getQueueAttributes(new GetQueueAttributesRequest(queueUrl).withAttributeNames(key));
        return approximateNumberOfMessages.getAttributes().get(key);
    }

    /**
     *  an AmazonServiceException exception is thrown if the queue doesn't exist
     */
    public static String getQueueUrlByName (String name) {

        return sqs.getQueueUrl(name).getQueueUrl();
    }
    public static List<String> listQueues(){
        ListQueuesResult listQueuesResult = sqs.listQueues();

        return listQueuesResult.getQueueUrls();
    }


    public static void init() {
        if ("DEV-LOCAL".equals(System.getenv("DSP_MODE")) || "DEV-LOCAL".equals(System.getenv("DSP_MODE_SQS"))){
            String host = "localhost";
            int port = 4568;
            String URL = "http://" + host + ":" + port;
            logger.info("Using development SQS with url " + URL);
            sqs.setEndpoint(URL);
        }
        else if ("DEV".equals(System.getenv("DSP_MODE")) || "DEV".equals(System.getenv("DSP_MODE_SQS"))) {
            logger.info("Using production SQS with local credentials");
            AWSCredentials credentials = Utils.getAwsCredentials();
            sqs = new AmazonSQSClient(credentials);
            sqs.setRegion(Utils.region);
        }
        else {
            logger.info("Using production SQS");
            // TODO shouldn't we set region?
            sqs = new AmazonSQSClient(new InstanceProfileCredentialsProvider());
        }
    }


}
