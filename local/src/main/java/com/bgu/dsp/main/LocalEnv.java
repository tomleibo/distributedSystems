package com.bgu.dsp.main;

import com.bgu.dsp.awsUtils.Utils;
import com.sun.corba.se.impl.javax.rmi.CORBA.Util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by thinkPAD on 4/4/2016.
 */
public class LocalEnv {
    public static final String BUCKET_NAME = Utils.LOCAL_TO_MANAGER_BUCKET_NAME;
    public static final String INPUT_FILE_KEY = "inputFile";
    public static final String LOCAL_TO_MANAGER_QUEUE_NAME = Utils.LOCAL_TO_MANAGER_QUEUE_NAME;
    public static final int N_THREADS = 4;

    private static LocalEnv instance = null;

    public String inputFileName;
    public String outputFileName;
    public String inQueueName;
    public float filesToWorkersRatio;
    public boolean terminate;
    public ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);
    public String outQueueUrl;
    public String inQueueUrl;
    public LocalMachine.HeartBit heartBit;

    public static LocalEnv build() {
        if (instance==null) {
            synchronized (LocalEnv.class) {
                if (instance==null) {
                    instance = new LocalEnv();
                }
            }
        }
        return instance;
    }

    public static LocalEnv get() {
        return instance;
    }

}
