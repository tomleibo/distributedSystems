package com.bgu.dsp.main;

import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerCommand;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerSQSProtocol;

/**
 TODO pass number of tweets.
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

    //format of sqs msg?
    //file format?

    private static void terminateManager() {

    }

    /**

     *
     *
     * add heartbit thread.
     */


    private void terminate() {
        String messageBody = LocalToManagerSQSProtocol.newTerminateMessage();

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
