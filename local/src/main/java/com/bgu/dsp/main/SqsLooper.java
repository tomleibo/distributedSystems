package com.bgu.dsp.main;

import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.common.protocol.MalformedMessageException;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerSQSProtocol;
import com.bgu.dsp.common.protocol.managertolocal.ManagerToLocalSqsProtocol;
import com.bgu.dsp.common.protocol.managertolocal.NewLocalCommand;

import java.util.concurrent.ExecutorService;

/**
 TODO shutdown heartbit thread from here. need to redesign.
 */
public class SqsLooper implements Runnable {
    private static final int SLEEP_CYCLE = 10;
    private static final long SQS_LOOP_SLEEP_DURATION_MILLIS = 1000 * 10;
    private static final long DELAY_BETWEEN_TERMINATE_MESSAGE_AND_SHUTDOWN = 1000 * 60;

    private boolean terminateWhenFinished;
    private String queueUrl;
    private ExecutorService executor;

    public SqsLooper(boolean terminateWhenFinished, String queueUrl, ExecutorService executor) {
        this.terminateWhenFinished = terminateWhenFinished;
        this.queueUrl=queueUrl;
        this.executor =executor;
    }

    private void terminate() {
        if (terminateWhenFinished) {
            String messageBody = LocalToManagerSQSProtocol.newTerminateMessage();
            SQSUtils.sendMessage(queueUrl,messageBody);
            try {
                Thread.sleep(DELAY_BETWEEN_TERMINATE_MESSAGE_AND_SHUTDOWN);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void run() {
        do {
            String msg = SQSUtils.getMessage(queueUrl).getBody();
            if (msg!=null) {
                try {
                    NewLocalCommand cmd = ManagerToLocalSqsProtocol.parse(msg);
                    executor.execute(cmd);
                    break;
                }
                catch (MalformedMessageException e) {
                    malFormedMessage(msg,e);
                }
            }
            try {
                Thread.sleep(SQS_LOOP_SLEEP_DURATION_MILLIS);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while(true);
        terminate();
    }

    private void malFormedMessage(String msg, MalformedMessageException e) {
        //TODO
    }

}
