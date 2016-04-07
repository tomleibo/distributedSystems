package com.bgu.dsp.awsUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

/**
 * Created by thinkPAD on 3/29/2016.
 */
public class Utils {
    public static final String WORKER_IMAGE_ID = "ami-b66ed3de";
    public static final String WORKERS_SECURITY_GROUP = "workers_security_group";
    public static final String MANAGER_INSTANCE_NAME = "manager";
    public static final String MANAGER_SECURITY_GROUP = "manager_security_group";
    public static Region region = Region.getRegion(Regions.US_EAST_1);

	/**
	 * Queue in which the manager passes tasks to the workers
     */
    public static final String MANAGER_TO_WORKERS_QUEUE_NAME = "manager-to-workers-queue";

	/**
     * Queue in which the local passes tasks to the manager
     */
    public static final String LOCAL_TO_MANAGER_QUEUE_NAME = "local-to-manager-queue";

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
