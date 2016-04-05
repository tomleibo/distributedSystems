package com.bgu.dsp.main;

import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.common.protocol.MalformedMessageException;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerCommand;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerSQSProtocol;
import com.bgu.dsp.common.protocol.managertolocal.ManagerToLocalSqsProtocol;
import com.bgu.dsp.common.protocol.managertolocal.NewLocalCommand;

/**
 TODO pass number of tweets WHAT?.
 TODO shutdown heartbit thread from here. need to redesign.
 */
public class SqsLooper implements Runnable {
    private static final int SLEEP_CYCLE = 10;
    private static final long SLEEP_DURATION_MILLIS = 1000 * 10;

    private boolean terminateWhenFinished;
    private Thread thread;
    private String queueUrl;

    public SqsLooper(boolean terminateWhenFinished, String queueUrl) {
        this.terminateWhenFinished = terminateWhenFinished;
        this.queueUrl=queueUrl;
    }

    private static void terminateManager() {

    }

    /**
     *
     * add heartbit thread.
     */


    private void terminate() {
        String messageBody = LocalToManagerSQSProtocol.newTerminateMessage();
        SQSUtils.sendMessage(queueUrl,messageBody);
    }

    @Override
    public void run() {
        do {
            String msg = SQSUtils.getMessage(queueUrl).getBody();
            if (msg!=null) {
                try {
                    NewLocalCommand cmd = ManagerToLocalSqsProtocol.parse(msg);
                    thread = new Thread(cmd);
                    thread.start();
                    break;
                }
                catch (MalformedMessageException e) {
                    malFormedMessage(msg,e);
                }
            }
            try {
                Thread.sleep(SLEEP_DURATION_MILLIS);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while(true);
    }

    private void malFormedMessage(String msg, MalformedMessageException e) {
        //TODO
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }
}
