package com.bgu.dsp.worker;

import com.amazonaws.services.sqs.model.Message;
import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.awsUtils.Utils;
import com.bgu.dsp.common.MessageKeepAlive;
import com.bgu.dsp.common.protocol.MalformedMessageException;
import com.bgu.dsp.common.protocol.managertoworker.ManagerToWorkerCommand;
import com.bgu.dsp.common.protocol.managertoworker.ManagerToWorkersSQSProtocol;
import com.bgu.dsp.worker.parser.NLPParser;
import org.apache.log4j.Logger;

import java.util.UUID;

public class Worker implements Runnable{
    final static Logger log = Logger.getLogger(Worker.class);
    private final String inQueueUrl;
    private UUID uuid = UUID.randomUUID();

    public static void main(String args[]) {
        Worker worker = new Worker();
        worker.run();
    }

    public Worker() {
        inQueueUrl = SQSUtils.getQueueUrlByName(Utils.MANAGER_TO_WORKERS_QUEUE_NAME);
    }

    @Override
    public void run() {
        while (true) {
            Message msg = getSqsMessageFromQueue();
            if (msg != null) {

                // Start the keepalive thread that makes sure that as long as the message is processed by this worker
                // it won't get back in to the queue
                Thread msgKeepAlive = new Thread(new MessageKeepAlive(msg, inQueueUrl, 30));
                msgKeepAlive.start();

                ManagerToWorkerCommand cmd = null;
                try {
                    cmd = ManagerToWorkersSQSProtocol.parse(msg.getBody());
                } catch (MalformedMessageException e) {
                    log.error("failed to parse message", e);
                }
                if (cmd != null) {
                    cmd.execute( new NLPParser(), this.uuid);
                    msgKeepAlive.interrupt();
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
