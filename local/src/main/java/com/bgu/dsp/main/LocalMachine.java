package com.bgu.dsp.main;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.sqs.model.QueueNameExistsException;
import com.bgu.dsp.awsUtils.EC2Utils;
import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerSQSProtocol;

import java.io.File;
import java.util.UUID;

/**
 * Created by thinkPAD on 4/10/2016.
 */
public class LocalMachine implements Runnable{

    LocalEnv env;

    public LocalMachine() {
        env = LocalEnv.get();
    }

    @Override
    public void run() {
        uploadInputFile(LocalEnv.get().inputFileName);
        getQueueUrlOrCreateIfNotExists();
        sendMessageToManager();
        if (!isManagerNodeActive()) {
            startManager();
        }
        startHeartBit();
        env = LocalEnv.get();
        createSqsLooper();
        
    }

    private  void startHeartBit() {
        env.executor.execute(new HeartBit() {

        });
    }


    private  void getQueueUrlOrCreateIfNotExists() {
        try {
            env.outQueueUrl = SQSUtils.createQueue(LocalEnv.LOCAL_TO_MANAGER_QUEUE_NAME);
        }
        catch(QueueNameExistsException e) {
            env.outQueueUrl =SQSUtils.getQueueUrlByName(LocalEnv.LOCAL_TO_MANAGER_QUEUE_NAME);
        }
        env.inQueueName = UUID.randomUUID().toString();
        env.inQueueUrl = SQSUtils.createQueue(env.inQueueName);
    }

    private  void sendMessageToManager() {
        String messageBody = LocalToManagerSQSProtocol.newTaskMessage(env.inQueueName, LocalEnv.BUCKET_NAME, LocalEnv.INPUT_FILE_KEY, env.terminate);
        boolean messageSent = SQSUtils.sendMessage(env.outQueueUrl, messageBody);
        if (!messageSent) {
            sqsMessageNotSent(env.outQueueUrl,messageBody);
        }
    }

    private  Bucket getOrCreateBucketByName(String bucketName) {
        Bucket bucket=null;
        for (Bucket b : S3Utils.getBuckets()) {
            if (b.getName().equals(bucketName)) {
                bucket=b;
                break;
            }
        }
        if (bucket ==null) {
            bucket=S3Utils.createBucket(bucketName);
        }
        return bucket;
    }

    private  void uploadInputFile(String inputFileName) {
        Bucket bucket = getOrCreateBucketByName(LocalEnv.BUCKET_NAME);
        File inFile = new File(inputFileName);
        if (!S3Utils.uploadFile(bucket, LocalEnv.INPUT_FILE_KEY,inFile)){
            fileUploadFailed(bucket,inFile, LocalEnv.INPUT_FILE_KEY);
        }
    }

    private  void createSqsLooper() {
        SqsLooper looper =new SqsLooper();
        env.executor.execute(looper);
    }

    /**
     *
     * @return instance id
     */
    private  String startManager() {
        return EC2Utils.startManager();
    }


    public  boolean isManagerNodeActive() {
        Instance ins  = EC2Utils.getManagerInstance();
        if (ins==null) {
            return false;
        }
        return true;
    }

    private  void fileUploadFailed(Bucket bucket, File inFile, String inputFileKey) {
        throw new RuntimeException("Tweet-File upload failed");
    }

    private  void sqsMessageNotSent(String queueUrl, String messageBody) {
        throw new RuntimeException("Sqs message sending failed. url: "+queueUrl+" body:\n"+messageBody);
    }

    private class HeartBit implements Runnable {
        @Override
        public void run() {
            if (!isManagerNodeActive()) {
                startManager();
                //TODO is this necessary?
                sendMessageToManager();
            }
        }
    }
}
