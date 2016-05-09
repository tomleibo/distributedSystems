package com.bgu.dsp.main;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.model.Message;
import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.awsUtils.Utils;
import com.bgu.dsp.common.protocol.MalformedMessageException;
import com.bgu.dsp.common.protocol.localtomanager.LocalToManagerSQSProtocol;
import com.bgu.dsp.common.protocol.managertolocal.ManagerToLocalSqsProtocol;
import com.bgu.dsp.common.protocol.managertolocal.TweetsToHtmlConverter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

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
        sendTerminationMessage();
        SQSUtils.deleteQueue(env.inQueueUrl);
        env.executor.shutdownNow();
    }
    private void sendTerminationMessage() {
        String messageBody = LocalToManagerSQSProtocol.newTerminateMessage(env.inQueueName);
        boolean messageSent = SQSUtils.sendMessage(env.outQueueUrl, messageBody);
        if (!messageSent) {
            terminationMessageNotSent(env.outQueueUrl, messageBody);
        }
    }

    private void terminationMessageNotSent(String outQueueUrl, String messageBody) {
        log.error("termination message to manager not sent.");
    }

    @Override
    public void run() {
        log.info("starting sqs looper");
        do {
            Message msg=null;
            try {
                msg = SQSUtils.getMessage(env.inQueueUrl, 20);
            }
            catch(AmazonClientException e) {
                log.error("failed to fetch sqs message: "+e);
            }
            if (msg!=null) {
                try {
                    TweetsToHtmlConverter converter =ManagerToLocalSqsProtocol.parse(msg.getBody());
                    converter.execute(env.outputFileName);
                    if (env.terminate) {
                        // Expect a statistics file
                        String statsFile = downloadStatsFile();
                        if (statsFile != null) {
                            log.info("Stats file located at " + statsFile);
                        }
                        else {
                            log.info("download statistics file failed.");
                        }
                    }
                    break;
                }
                catch (MalformedMessageException e) {
                    malFormedMessage(msg.getBody(),e);
                }
                try {
                    SQSUtils.deleteMessage(env.inQueueUrl, msg);
                }
                catch(AmazonClientException e)  {
                    log.error("failed to delete manager to local sqs message.",e);
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

    private String downloadStatsFile() {
        // TODO how to know when did the manager has finished to upload the statistics file?
        Message message=null;
        try {
            message = SQSUtils.getMessage(env.inQueueUrl, 20);
        }
        catch(AmazonClientException e) {
            log.error("Failed to fetch statistics file location sqs message"+e);
        }

        if (message != null && (! "NO_STATS_FILE".equals(message.getBody()))) {
			try {
                File file = S3Utils.downloadFile(Utils.MANAGER_TO_LOCAL_BUCKET_NAME,
                        message.getBody());
                return file.getAbsolutePath();
            }
            catch (IOException | AmazonClientException e) {
				log.error("Could not get statistics file", e);
			}
		}
		else {
			log.error("Timed out when waiting for statistics file from manager");
		}
        return null;
    }


    private void malFormedMessage(String msg, MalformedMessageException e) {
        log.error("malformed message:",e);
    }



}
