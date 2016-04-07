package com.bgu.dsp.main;

import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.common.protocol.MalformedMessageException;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerSQSProtocol;
import com.bgu.dsp.common.protocol.managertolocal.ManagerToLocalSqsProtocol;
import com.bgu.dsp.common.protocol.managertolocal.TweetsToHtmlConverter;

import java.io.*;
import java.util.concurrent.ExecutorService;

public class SqsLooper implements Runnable {
    private static final long SQS_LOOP_SLEEP_DURATION_MILLIS = 1000 * 10;
    private static final long DELAY_BETWEEN_TERMINATE_MESSAGE_AND_SHUTDOWN = 1000 * 60;

    private final String outputFileName;
    private final boolean terminateWhenFinished;
    private final String queueUrl;
    private final ExecutorService executor;

    public SqsLooper(boolean terminateWhenFinished, String queueUrl, ExecutorService executor, String outputFileName) {
        this.terminateWhenFinished = terminateWhenFinished;
        this.queueUrl=queueUrl;
        this.executor =executor;
        this.outputFileName= outputFileName;
    }

    private void finish() {
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
        executor.shutdownNow();
    }

    @Override
    public void run() {
        do {
            String msg = SQSUtils.getMessage(queueUrl).getBody();
            if (msg!=null) {
                try {
                    TweetsToHtmlConverter converter =ManagerToLocalSqsProtocol.parse(msg);
                    String html = converter.convert();
                    writeToFile(html);
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
        finish();
    }

    private void writeToFile(String html) {
        File file = new File(outputFileName);
        if (!file.exists()) {
            file.mkdirs();
            try {
                file.createNewFile();
            }
            catch (IOException e) {
                outputFileNotCreated(file,e);
            }
        }
        try (PrintWriter writer = new PrintWriter(outputFileName, "UTF-8")) {
            writer.println(html);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void malFormedMessage(String msg, MalformedMessageException e) {
        //TODO
    }

    private void outputFileNotCreated(File file, IOException e) {
        //TODO
    }

}
