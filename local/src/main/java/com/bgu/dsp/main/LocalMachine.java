package com.bgu.dsp.main;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.sqs.model.QueueNameExistsException;
import com.bgu.dsp.awsUtils.EC2Utils;
import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerSQSProtocol;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import java.io.File;
import java.util.UUID;

/**
 * Created by thinkPAD on 4/10/2016.
 */
public class LocalMachine implements Runnable{
    final static Logger log = Logger.getLogger(LocalMachine.class);
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
        String messageBody = LocalToManagerSQSProtocol.newTaskMessage(env.inQueueName, LocalEnv.BUCKET_NAME, LocalEnv.INPUT_FILE_KEY, env.terminate,env.filesToWorkersRatio);
        boolean messageSent = SQSUtils.sendMessage(env.outQueueUrl, messageBody);
        if (!messageSent) {
            sqsMessageNotSent(env.outQueueUrl,messageBody);
        }
        log.log(Priority.INFO,"message sent to server: "+messageBody);
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
        log.log(Priority.INFO,"upload successful: "+inFile.getPath());
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
            log.log(Priority.WARN,"manager is not active. activating... ");
            return false;
        }
        return true;
    }

    private  void fileUploadFailed(Bucket bucket, File inFile, String inputFileKey) {
        String message = "Tweet-File upload failed";
        log.log(Priority.ERROR,message);
        throw new RuntimeException(message);
    }

    private  void sqsMessageNotSent(String queueUrl, String messageBody) {
        String message = "Sqs message sending failed. url: " + queueUrl + " body:\n" + messageBody;
        log.log(Priority.ERROR,message);
        throw new RuntimeException(message);
    }

    private class HeartBit implements Runnable {
        final int SLEEP_CYCLE = 1000*10;

        @Override
        public void run() {
            try {
                log.log(Priority.INFO,"heartbit started.");
                while (true) {
                    if (!isManagerNodeActive()) {
                        startManager();
                        //TODO is this necessary?
                        sendMessageToManager();
                    }
                    Thread.sleep(SLEEP_CYCLE);
                }
            }
            catch (InterruptedException e) {
                log.log(Priority.INFO,"heartbit interrupted",e);
            }

        }
    }
}
