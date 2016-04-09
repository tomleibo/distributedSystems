package com.bgu.dsp.awsUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class EC2Utils {

    private static int
            STATE_CODE_PENDING = 0,
            STATE_CODE_RUNNING = 16,
            STATE_CODE_SHUTTING_DOWN = 32,
            STATE_CODE_TERMINATED = 48,
            STATE_CODE_STOPPING = 64,
            STATE_CODE_STOPPED = 80;

    private static AmazonEC2Client ec2;
    final static Logger logger = Logger.getLogger(EC2Utils.class);

    static {
        init();
    }

    public static void init() {
        AWSCredentials credentials = Utils.getAwsCredentials();
        ec2 = new AmazonEC2Client(credentials);
        ec2.setRegion(Utils.region);

        if ("DEV".equals(System.getenv("DSP_MODE")) || "DEV".equals(System.getenv("DSP_MODE_EC2"))){
            final String URL = "http://localhost:8000/aws-mock/ec2-endpoint/";
            ec2.setEndpoint(URL);
            logger.info("Using development EC2 with url " + URL);
        }
    }

    /**
     * Count machines who's name matches *worker* and that are either pending or running
     * @return
     */
    public static int countWorkers(){
        int instanceCount = 0;
        DescribeInstancesResult describeInstancesResult = getAllWorkers();
        for (Reservation reservation : describeInstancesResult.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                System.out.println(instance);
                if (instance.getState().getCode() == STATE_CODE_PENDING ||
                        instance.getState().getCode() == STATE_CODE_RUNNING){
                    System.out.println(instance.getTags());
                    instanceCount++;
                }
            }
        }

        return instanceCount;
    }

    private static DescribeInstancesResult getAllWorkers() {
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();

        ArrayList<Filter> filters = new ArrayList<>();

        // Filter only for running or pending nodes
        ArrayList<String> stateFilterValues = new ArrayList<>();
        stateFilterValues.add(Integer.toString(STATE_CODE_PENDING));
        stateFilterValues.add(Integer.toString(STATE_CODE_RUNNING));
        Filter stateFilter = new Filter("instance-state-code", stateFilterValues);
        filters.add(stateFilter);

        // Filter only for nodes called *worker*
        ArrayList<String> nameFilterValues = new ArrayList<>();
        nameFilterValues.add("*worker*");
        Filter nameFilter = new Filter("tag:Name", nameFilterValues);
        filters.add(nameFilter);

        describeInstancesRequest.setFilters(filters);


        return ec2.describeInstances(describeInstancesRequest);
    }

    /**
     * Start n workers and wait for them to be in "Running" status
     */
    public static void startWorkersAndWait(int n){
        List<String> ids = startWorkers(n);
        waitForRunning(ids);
    }

    /**
     * Start n workers
     * @see #startWorkersAndWait
     */
    public static List<String> startWorkers(int n){
        // TODO create a workers security group in AWS
        RunInstancesRequest request = new RunInstancesRequest().
                withImageId(Utils.WORKER_IMAGE_ID).
                withMinCount(n).
                withMaxCount(n).
                withSecurityGroups(Utils.WORKERS_SECURITY_GROUP).
                withInstanceType(Utils.WORKER_INSTANCE_TYPE).
                withSecurityGroups(Utils.WORKERS_SECURITY_GROUP);
        RunInstancesResult runInstancesResult = ec2.runInstances(request);

        List<String> instancesIds = runInstancesResult.getReservation().getInstances().stream().map(Instance::getInstanceId).collect(Collectors.toList());

        tagWorkers(instancesIds);
        return instancesIds;
    }

    private static void tagWorkers(List<String> instancesIds) {
        List<Tag> tags = new LinkedList<>();
        tags.add(new Tag("Name", "worker"));
        CreateTagsRequest createTagsRequest = new CreateTagsRequest(instancesIds, tags);
        ec2.createTags(createTagsRequest);
    }

    /**
     * Wait for a list of instances
     */
    private static void waitForRunning(List<String> instancesIds){
        logger.debug("Waiting for " + instancesIds.size() + " machines to start");
        long startTime = System.currentTimeMillis();

        boolean allRunning = false;

        while (!allRunning) {
            DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
            describeInstancesRequest.setInstanceIds(instancesIds);
            DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);

            allRunning = checkIfAllRunning(describeInstancesResult);

            if (! allRunning) {
                try {
                    Thread.sleep(3 * 1000);
                } catch (InterruptedException e) {
                    //continue
                }
            }
        }

        long end = System.currentTimeMillis();
        logger.debug("Waited for instances to start " + (end-startTime) + " milliseconds");
    }

    /**
     * Return true only if all the instances are running
     */
    private static boolean checkIfAllRunning(DescribeInstancesResult describeInstancesResult) {
        for (Reservation reservation : describeInstancesResult.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                if (instance.getState().getCode() != STATE_CODE_RUNNING){
                    return false;
                }
            }
        }
        return true;
    }

    public static Instance getManagerInstance() {

        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();

        ArrayList<Filter> filters = new ArrayList<>();

        // Filter only for running or pending nodes
        ArrayList<String> stateFilterValues = new ArrayList<>();
        stateFilterValues.add(Integer.toString(STATE_CODE_PENDING));
        stateFilterValues.add(Integer.toString(STATE_CODE_RUNNING));
        Filter stateFilter = new Filter("instance-state-code", stateFilterValues);
        filters.add(stateFilter);

        ArrayList<String> nameFilterValues = new ArrayList<>();
        nameFilterValues.add(Utils.MANAGER_INSTANCE_NAME);
        Filter nameFilter = new Filter("tag:Name", nameFilterValues);
        filters.add(nameFilter);

        describeInstancesRequest.setFilters(filters);
        DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);
        for (Reservation reservation : describeInstancesResult.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                return instance;
            }
        }
        return null;
    }

    /**
     * @return instance ID
     */
    public static String startManager() {
        RunInstancesRequest request = new RunInstancesRequest().
                withImageId(Utils.WORKER_IMAGE_ID).
                withMinCount(1).
                withMaxCount(1).
                withSecurityGroups(Utils.MANAGER_SECURITY_GROUP).
                withInstanceType(InstanceType.T2Micro);
        RunInstancesResult runInstancesResult = ec2.runInstances(request);

        String instancesIds = runInstancesResult.getReservation().getInstances().get(0).getInstanceId();
        return instancesIds;
    }


    public static void terminateAllWorkers() {
        List<String> workesrIDs = new LinkedList<>();

        DescribeInstancesResult allWorkers = getAllWorkers();
        for (Reservation reservation : allWorkers.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                workesrIDs.add(instance.getInstanceId());
            }
        }
        if (workesrIDs.size() == 0) {
            logger.info("No worker discoverd, skipping shutdown");
            return;
        }
        else {
            logger.debug("Terminating " + workesrIDs.size() + " workers");
        }

        TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest(workesrIDs);
        ec2.terminateInstances(terminateInstancesRequest);
    }
}
