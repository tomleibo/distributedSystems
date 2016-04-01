package com.bgu.dsp.awsUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class EC2Utils {
    private static final long SLEEP_CYCLE = 60000;
    private static AmazonEC2 ec2 = null;



    static {
        init();
    }

    public static void main(String[] args) throws Exception {

        try {
            Collection<String> securityGroups = Arrays.asList(new String[]{"default"});
            List<String> requestIds = submitRequests("0.03", Integer.valueOf(1), "ami-8c1fece5", "t1.micro", securityGroups);

            // Loop through all of the instanceRequests until all bids are in the active state
            // (or at least not in the open state).
            boolean isOpen = false;
            List<SpotInstanceRequest> requests=null;
            List<String> activeInsanceIds = new ArrayList<>();
            do {
                // Sleep for 60 seconds.
                Thread.sleep(SLEEP_CYCLE);
                requests = getDeliveredInstanceRequestsByIds(requestIds);

                for (SpotInstanceRequest request : requests) {
                    if (request.getState().equals("open")) {
                        isOpen=true;
                    }
                    else if (request.getState().equals("active")) {
                        activeInsanceIds.add(request.getInstanceId());
                    }
                }
            } while (isOpen);


            // Cancel all instanceRequests and terminate all running instances.
            cleanup(requestIds,activeInsanceIds);

        } catch (AmazonServiceException ase) {
            // Write out any exceptions that may have occurred.
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public static void init() {
        ec2 = new AmazonEC2Client(Utils.getAwsCredentials());
        ec2.setRegion(Utils.region);
    }

    /**
     * @param spotPrice (0.03)
     * @param instanceCount 1
     * @param imageId ("ami-8c1fece5")
     * @param instanceType "t1.micro"
     * @param securityGroups ["GettingStartedGroup"]
     * @return request Ids as string list.
     */
    public static List<String> submitRequests(String spotPrice, Integer instanceCount, String imageId, String instanceType, Collection<String> securityGroups) {
        // Initializes a Spot Instance Request
        RequestSpotInstancesRequest requestRequest = new RequestSpotInstancesRequest();
        requestRequest.setSpotPrice(spotPrice);
        requestRequest.setInstanceCount(instanceCount);

        // Setup the specifications of the launch. This includes the instance type (e.g. t1.micro)
        // and the latest Amazon Linux AMI id available. Note, you should always use the latest
        // Amazon Linux AMI id or another of your choosing.
        LaunchSpecification launchSpecification = new LaunchSpecification();
        launchSpecification.setImageId(imageId);
        launchSpecification.setInstanceType(instanceType);
        launchSpecification.setSecurityGroups(securityGroups);
        requestRequest.setLaunchSpecification(launchSpecification);

        // Call the RequestSpotInstance API.
        RequestSpotInstancesResult requestResult = ec2.requestSpotInstances(requestRequest);
        List<SpotInstanceRequest> requestResponses = requestResult.getSpotInstanceRequests();

        List<String> spotInstanceRequestIds = new ArrayList<>();
        for (SpotInstanceRequest requestResponse : requestResponses) {
            System.out.println("Created Spot Request: " + requestResponse.getSpotInstanceRequestId());
            spotInstanceRequestIds.add(requestResponse.getSpotInstanceRequestId());
        }
        return spotInstanceRequestIds;
    }

    /**
     * The areOpen method will determine if any of the instanceRequests that were started are still
     * in the open state. If all of them have transitioned to either active, cancelled, or
     * closed, then this will return false.
     *
     * @return
     */
    public static List<SpotInstanceRequest> getDeliveredInstanceRequestsByIds(List<String> spotInstanceRequestIds) {
        List<SpotInstanceRequest> instanceRequests = new ArrayList<>();

        // Create the describeRequest with tall of the request id to monitor (e.g. that we started).
        DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();
        describeRequest.setSpotInstanceRequestIds(spotInstanceRequestIds);

        try {
            // Retrieve all of the instanceRequests we want to monitor.
            DescribeSpotInstanceRequestsResult describeResult = ec2.describeSpotInstanceRequests(describeRequest);
            List<SpotInstanceRequest> describeResponses = describeResult.getSpotInstanceRequests();

            // Look through each request and determine if they are all in the active state.
            for (SpotInstanceRequest describeResponse : describeResponses) {
                System.out.println(" " + describeResponse.getSpotInstanceRequestId() +
                        " is in the " + describeResponse.getState() + " state.");
                instanceRequests.add(describeResponse);
            }
        } catch (AmazonServiceException e) {
            // Print out the error.
            System.out.println("Error when calling describeSpotInstances");
            System.out.println("Caught Exception: " + e.getMessage());
            System.out.println("Reponse Status Code: " + e.getStatusCode());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("Request ID: " + e.getRequestId());
        }
        return instanceRequests;
    }

    public static void cleanup(List<String> spotInstanceRequestIds, List<String> activeInstanceIds) {
        cancelRequestsByIds(spotInstanceRequestIds);
        terminateInstancesByIds(activeInstanceIds);
    }

    public static void terminateInstancesByIds(List<String> activeInstanceIds) {
        try {
            // Terminate instances.
            System.out.println("Terminate instances");
            TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest(activeInstanceIds);
            ec2.terminateInstances(terminateRequest);
        } catch (AmazonServiceException e) {
            // Write out any exceptions that may have occurred.
            System.out.println("Error terminating instances");
            System.out.println("Caught Exception: " + e.getMessage());
            System.out.println("Reponse Status Code: " + e.getStatusCode());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("Request ID: " + e.getRequestId());
        }
    }

    public static void cancelRequestsByIds(List<String> spotInstanceRequestIds) {
        try {
            // Cancel instanceRequests.
            System.out.println("Cancelling instanceRequests.");
            CancelSpotInstanceRequestsRequest cancelRequest = new CancelSpotInstanceRequestsRequest(spotInstanceRequestIds);
            ec2.cancelSpotInstanceRequests(cancelRequest);
        } catch (AmazonServiceException e) {
            // Write out any exceptions that may have occurred.
            System.out.println("Error cancelling instances");
            System.out.println("Caught Exception: " + e.getMessage());
            System.out.println("Reponse Status Code: " + e.getStatusCode());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("Request ID: " + e.getRequestId());
        }
    }

}



