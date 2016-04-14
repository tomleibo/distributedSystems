package com.bgu.dsp.main;

import com.amazonaws.services.sqs.model.Message;
import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.awsUtils.Utils;
import com.bgu.dsp.common.protocol.MalformedMessageException;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerSQSProtocol;
import com.bgu.dsp.common.protocol.managertolocal.ManagerToLocalSqsProtocol;
import com.bgu.dsp.common.protocol.managertolocal.TweetsToHtmlConverter;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import java.io.*;

public class SqsLooper implements Runnable {
    final static Logger log = Logger.getLogger(SqsLooper.class);
    private static final long SQS_LOOP_SLEEP_DURATION_MILLIS = 1000 * 10;
    private static final long DELAY_BETWEEN_TERMINATE_MESSAGE_AND_SHUTDOWN = 1000 * 60;
    private final LocalEnv env;

    public SqsLooper() {
        env = LocalEnv.get();
    }

    private void finish() {
        log.info("deleting queue and shutting down .");
        SQSUtils.deleteQueue(env.inQueueUrl);
        env.executor.shutdownNow();
    }

    @Override
    public void run() {
        log.info("starting sqs looper");
        do {
            Message msg = SQSUtils.getMessage(env.inQueueUrl);
            if (msg!=null) {
                try {
                    TweetsToHtmlConverter converter =ManagerToLocalSqsProtocol.parse(msg.getBody());
                    String html = converter.convert();
                    log.info("writing HTML to file: "+env.outputFileName);
                    writeToFile(html);
                    break;
                }
                catch (MalformedMessageException e) {
                    malFormedMessage(msg.getBody(),e);
                }
            }
            try {
                Thread.sleep(SQS_LOOP_SLEEP_DURATION_MILLIS);
            }
            catch (InterruptedException e) {
                log.warn("looper interrupted: ",e);
            }
        } while(true);
        finish();
    }

    private void writeToFile(String html) {
        File file = new File(env.outputFileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            }
            catch (IOException e) {
                outputFileNotCreated(file,e);
            }
        }
        try (PrintWriter writer = new PrintWriter(env.outputFileName, "UTF-8")) {
            writer.println(html);
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            log.error(e);
        }
    }

    private void malFormedMessage(String msg, MalformedMessageException e) {
        log.error("malformed message:",e);
    }

    private void outputFileNotCreated(File file, IOException e) {
        log.error("output file not created",e);
    }

}
