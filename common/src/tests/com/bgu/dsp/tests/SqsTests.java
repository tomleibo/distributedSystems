package com.bgu.dsp.tests;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.model.Message;
import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.bgu.dsp.awsUtils.SQSUtils.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by thinkPAD on 3/29/2016.
 */

public class SqsTests {
    @Test
    public void test () {
        try {

            //create queue
            System.out.println("Creating a new SQS queue called MyQueue.\n");
            String queueName = "MyQueue"+ UUID.randomUUID();
            String myQueueUrl = createQueue(queueName);
            assertTrue(getQueues().getQueueUrls().contains(myQueueUrl));

            // Send a message
            String messageBody = "This is my message text.";
            assertTrue(sendMessage(myQueueUrl, messageBody));

            // Receive messages
            System.out.println("Receiving messages from MyQueue.\n");
            List<Message> messages = getMessages(myQueueUrl);
            Boolean b= messages.stream().map(Message::getBody).collect(Collectors.toList()).contains(messageBody);
            assertTrue(b);

            // Delete a message
            System.out.println("Deleting a message.\n");
            Message message = messages.get(0);
            deleteMessage(myQueueUrl, message);
            messages = getMessages(myQueueUrl);
            b=messages.stream().map(Message::getBody).collect(Collectors.toList()).contains(messageBody);
            assertFalse(b);

            // Delete a queue
            System.out.println("Deleting the test queue.\n");
            deleteQueue(myQueueUrl);
            assertFalse(getQueues().getQueueUrls().contains(myQueueUrl));
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it " +
                    "to Amazon SQS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered " +
                    "a serious internal problem while trying to communicate with SQS, such as not " +
                    "being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }
}
