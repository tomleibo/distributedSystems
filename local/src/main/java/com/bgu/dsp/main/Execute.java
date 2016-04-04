package com.bgu.dsp.main;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.sqs.model.QueueNameExistsException;
import com.bgu.dsp.awsUtils.EC2Utils;
import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerSQSProtocol;

import java.io.File;

/**
 * Created by thinkPAD on 4/2/2016.
 */
public class Execute {

    /*
    Bucket names should not contain underscores
    Bucket names should be between 3 and 63 characters long
    Bucket names should not end with a dash
    Bucket names cannot contain adjacent periods
    Bucket names cannot contain dashes next to periods (e.g., "my-.bucket.com" and "my.-bucket" are invalid)
    Bucket names cannot contain uppercase characters
     */
    private static final String BUCKET_NAME = "bucket";
    private static final String INPUT_FILE_KEY = "inputFile";
    private static final String OUTPUT_FILE_KEY = "outputFile";
    private static final String LOCAL_TO_MANAGER_QUEUE_NAME = "localToManager";

    private static String inputFileName;
    private static String outputFileName;
    private static float filesToWorkersRatio;
    private static boolean terminate = false;


    public static void main(String args[]) {
        parseArgs(args);
        uploadInputFile(inputFileName);
        sendFileLocationToSqs();
        if (!isManagerNodeActive()) {
            startManager();
        }
        String queueUrl ="";
        createSqsLooper(queueUrl);
    }

    public static void parseArgs(String[] args) {
        if (args.length < 3) {
            throw new RuntimeException("usage: inputFileName outputFileName filesToWorkersRate [terminate]");
        }
        inputFileName = args[0];
        outputFileName = args[1];
        try {
            filesToWorkersRatio = Float.parseFloat(args[2]);
        }
        catch(NumberFormatException e ) {
            System.out.println("third argument should be a number.");
            e.printStackTrace();
            System.exit(1);
        }
        if (args.length > 3) {
            terminate = true;
        }
    }

    private static void sendFileLocationToSqs() {
        String queueUrl;
        try {
            queueUrl = SQSUtils.createQueue(LOCAL_TO_MANAGER_QUEUE_NAME);
        }
        catch(QueueNameExistsException e) {
            queueUrl=SQSUtils.getQueueUrlByName(LOCAL_TO_MANAGER_QUEUE_NAME);
        }
        String messageBody = LocalToManagerSQSProtocol.newTaskMessage(BUCKET_NAME,INPUT_FILE_KEY);
        boolean messageSent = SQSUtils.sendMessage(queueUrl,messageBody);
        if (!messageSent) {
            sqsMessageNotSent(queueUrl,messageBody);
        }
    }

    private static Bucket getOrCreateBucketByName(String bucketName) {
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

    private static void uploadInputFile(String inputFileName) {
        Bucket bucket = getOrCreateBucketByName(BUCKET_NAME);
        File inFile = new File(inputFileName);
        if (!S3Utils.uploadFile(bucket,INPUT_FILE_KEY,inFile)){
            fileUploadFailed(bucket,inFile,INPUT_FILE_KEY);
        }
    }

    private static void createSqsLooper(String queueUrl) {
        SqsLooper looper = new SqsLooper(terminate,queueUrl);
        looper.start();
    }

    private static void startManager() {
        String id = EC2Utils.startManager();
    }


    private static boolean isManagerNodeActive() {
        Instance ins  = EC2Utils.getManagerInstance();
        if (ins==null) {
            return false;
        }
        return true;
    }

    private static void fileUploadFailed(Bucket bucket, File inFile, String inputFileKey) {
        throw new RuntimeException("Tweet-File upload failed");
    }

    private static void sqsMessageNotSent(String queueUrl, String messageBody) {
        throw new RuntimeException("Sqs message sending failed. url: "+queueUrl+" body:\n"+messageBody);
    }
}
