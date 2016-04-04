package com.bgu.dsp.main;

import com.bgu.dsp.awsUtils.SQSUtils;

/**
 * Created by thinkPAD on 4/2/2016.
 */
public class SqsLooper implements Runnable {
    private static final int SLEEP_CYCLE = 10;

    private boolean terminateWhenFinished;
    private Thread thread;
    private String queueUrl;

    public SqsLooper(boolean terminateWhenFinished, String queueUrl) {
        this.terminateWhenFinished = terminateWhenFinished;
        this.queueUrl=queueUrl;

    }

    // this should create another thread which will check the SQS
    // occasionally and when found a message indicating the process is done
    // download the response and create an HTML file from it.
    //



    private static void terminateManager() {

    }


    @Override
    public void run() {
        do {
            SQSUtils.getMessage(queueUrl);
        } while(true);
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }
}
