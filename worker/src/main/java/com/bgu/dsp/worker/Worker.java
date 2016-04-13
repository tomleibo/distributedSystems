package com.bgu.dsp.worker;

import com.amazonaws.services.sqs.model.Message;
import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.awsUtils.Utils;
import com.bgu.dsp.common.protocol.MalformedMessageException;
import com.bgu.dsp.common.protocol.managertoworker.ManagerToWorkerCommand;
import com.bgu.dsp.common.protocol.managertoworker.ManagerToWorkersSQSProtocol;
import org.apache.log4j.Logger;

public class Worker implements Runnable{
    final static Logger log = Logger.getLogger(Worker.class);
    private final String inQueueUrl;

    public static void main(String args[]) {
        Worker worker = new Worker();
        worker.run();
    }

    public Worker() {
        inQueueUrl = SQSUtils.getQueueUrlByName(Utils.MANAGER_TO_WORKERS_QUEUE_NAME);
    }

    public void run() {
        while (true) {
            Message msg = getSqsMessageFromQueue();
            if (msg != null) {
                ManagerToWorkerCommand cmd = null;
                try {
                    cmd = ManagerToWorkersSQSProtocol.parse(msg.getBody());
                } catch (MalformedMessageException e) {
                    log.error("failed to parse message", e);
                }
                if (cmd != null) {
                    cmd.execute();
                    SQSUtils.deleteMessage(inQueueUrl, msg);
                }
            }
        }
    }


	/**
     * @return msg body if found a message in the queue or null if couldn't find a message in the queue
     */
    private Message getSqsMessageFromQueue() {
        return SQSUtils.getMessage(inQueueUrl, 20);
    }

}
