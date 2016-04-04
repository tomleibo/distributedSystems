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
    public static final String OUTPUT_FILENAME = "output.html";
    public static Region region = Region.getRegion(Regions.US_EAST_1);

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
}
