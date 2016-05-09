package com.bgu.dsp.main;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.sqs.model.QueueNameExistsException;
import com.bgu.dsp.awsUtils.EC2Utils;
import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerSQSProtocol;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.UUID;

public class LocalMachine implements Runnable{
    final static Logger log = Logger.getLogger(LocalMachine.class);
    private static final long RETRY_DURATION = 2000;
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
            log.info("Manager is not active. Stating the manager");
            startManager();
        }
        else {
            log.info("Using an already active manager");
        }
        startHeartBit();
        createSqsLooper();
    }

    private  void startHeartBit() {
        HeartBit heartbit = new HeartBit();
        env.heartBit=heartbit;
        env.executor.execute(heartbit);
    }


    private  void getQueueUrlOrCreateIfNotExists() {
        try {
            env.outQueueUrl = SQSUtils.createQueue(LocalEnv.LOCAL_TO_MANAGER_QUEUE_NAME);
        }
        catch(QueueNameExistsException e) {
            env.outQueueUrl =SQSUtils.getQueueUrlByName(LocalEnv.LOCAL_TO_MANAGER_QUEUE_NAME);
        }
        catch(AmazonClientException e2) {
            log.error("queue creation\\fetch failed. exiting. ",e2);
            System.exit(1);
        }
        env.inQueueName = UUID.randomUUID().toString();
        env.inQueueUrl = SQSUtils.createQueue(env.inQueueName);
    }

    private  void sendMessageToManager() {
        String messageBody = LocalToManagerSQSProtocol.newTaskMessage(env.inQueueName, LocalEnv.BUCKET_NAME, LocalEnv.INPUT_FILE_KEY,env.filesToWorkersRatio);
        boolean messageSent = SQSUtils.sendMessage(env.outQueueUrl, messageBody);
        if (!messageSent) {
            sqsMessageNotSent(env.outQueueUrl,messageBody);
        }
        log.info("message sent to server: "+messageBody);

    }



    private  Bucket getOrCreateBucketByName(String bucketName) {
        Bucket bucket=null;
        for (Bucket b : S3Utils.getBuckets()) {
            if (b.getName().equals(bucketName)) {
                bucket=b;
                break;
            }
        }
        if (bucket == null) {
            bucket=S3Utils.createBucket(bucketName);
        }
        return bucket;
    }

    private  void uploadInputFile(String inputFileName) {
        Bucket bucket = getOrCreateBucketByName(LocalEnv.BUCKET_NAME);
        File inFile = new File(inputFileName);
        if (!S3Utils.uploadFile(bucket, LocalEnv.INPUT_FILE_KEY,inFile)){
            fileUploadFailed(inputFileName);
        }
        log.info("upload successful: "+inFile.getPath());
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
        Instance ins=null;
        try {
            ins = EC2Utils.getManagerInstance();
        }
        catch(AmazonClientException e) {
            log.error("Ec2 call failed. Failed to check if manager is up.",e);
        }
        if (ins==null) {
            return false;
        }
        return true;
    }

    private  void fileUploadFailed(String inFile) {
        String message = "Tweet-File ("+inFile+") upload failed. exiting";
        log.error(message);
        System.out.println(message);
        System.exit(1);
    }

    private  void sqsMessageNotSent(String queueUrl, String messageBody) {
        String message = "Sqs message sending failed. Exiting. \nDetails: url: " + queueUrl + " body:\n" + messageBody;
        log.error(message);
        System.exit(1);
    }

    public class HeartBit implements Runnable {
        final int SLEEP_CYCLE = 1000*10;
        private boolean shouldStop=false;

        @Override
        public void run() {
            try {
                log.info("heartbit started.");
                while (!shouldStop) {
                    try {
                        if (!isManagerNodeActive()) {
                            log.info("Heartbeat waiting for the manager to become active");
                            Thread.sleep(1000 * 120);
                            if (!isManagerNodeActive() || shouldStop()) {
                                log.warn("Manager is not active for 2 minutes, heartbeat is activating the manager");
                                startManager();
                            }
                        }
                    }
                    catch(AmazonClientException e) {
                        log.error("Heartbit check Failed. AmazonClientException has been thrown:",e);
                    }
                    Thread.sleep(SLEEP_CYCLE);
                }
            }
            catch (InterruptedException e) {
                log.info("heartbit interrupted",e);
            }

        }

        private boolean shouldStop() {
            return shouldStop;
        }

        public void stop(){
            shouldStop=true;
        }
    }
}
