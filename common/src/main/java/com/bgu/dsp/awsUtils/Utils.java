package com.bgu.dsp.awsUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;

/**
 * Created by thinkPAD on 3/29/2016.
 */
public class Utils {
    public static final String MANAGER_IMAGE_ID = "ami-a78499cd";
    public static final String WORKER_IMAGE_ID = MANAGER_IMAGE_ID;
    public static final String WORKERS_SECURITY_GROUP = "workers_security_group";
    public static final String MANAGER_INSTANCE_NAME = "manager";
    public static final String MANAGER_SECURITY_GROUP = "manager_security_group";
    public static final InstanceType WORKER_INSTANCE_TYPE = InstanceType.T2Micro;

	/**
	 * This can be a relatively large number as it only represents the number of tasks that were
     * acquired from the queue, and not the number of tasks that are actually running
     */
    public static final int NUM_OF_MANAGER_TASKS = 500;
    public static Region region = Region.getRegion(Regions.US_EAST_1);

	/**
	 * Queue in which the manager passes tasks to the workers
     */
    public static final String MANAGER_TO_WORKERS_QUEUE_NAME = "manager-to-workers-queue";

	/**
     * Queue in which the local passes tasks to the manager
     */
    public static final String LOCAL_TO_MANAGER_QUEUE_NAME = "local-to-manager-queue";

	/**
	 * The bucket in which the manager is sending results to the local
     */
    public static final String MANAGER_TO_LOCAL_BUCKET_NAME = "manager-to-local-bucket";

    public static final String LOCAL_TO_MANAGER_BUCKET_NAME = "local-to-manager-bucket";

    public static AWSCredentials getAwsCredentials() {
        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        }
        catch (Exception e) {
            throw new AmazonClientException("Cannot load the credentials from the credential profiles file. ",e);
        }
        return credentials;
    }

    public static class Pair<K,V> {
        K key;
        V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
