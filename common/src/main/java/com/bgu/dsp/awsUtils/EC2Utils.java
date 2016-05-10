package com.bgu.dsp.awsUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.bgu.dsp.awsUtils.exceptions.NoSuchInstanceException;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.util.*;
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

        if ("DEV-LOCAL".equals(System.getenv("DSP_MODE")) || "DEV-LOCAL".equals(System.getenv("DSP_MODE_EC2"))){
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
                if (instance.getState().getCode() == STATE_CODE_PENDING ||
                        instance.getState().getCode() == STATE_CODE_RUNNING){
                    instanceCount++;
                }
            }
        }

        return instanceCount;
    }

    public static List<String> getAllWorkersIds(){
        List<String> res = new LinkedList<>();
        DescribeInstancesResult allWorkers = getAllWorkers();
        for (Reservation reservation : allWorkers.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                res.add(instance.getInstanceId());
            }
        }
        return res;
    }

    public static InstanceState getInstanceState(String instanceId) throws NoSuchInstanceException {
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceId);
        DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);
        List<Reservation> reservations = describeInstancesResult.getReservations();
        if (reservations.size() < 1){
            throw new NoSuchInstanceException("There is no instance with id " + instanceId);
        }
        else{
            return reservations.get(0).getInstances().get(0).getState();
        }

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
        if (n < 0){
            throw new IllegalArgumentException("n must be >= 0. got n=" + n);
        }
        if (n == 0){
            // Can't send an empty RunInstanceRequest
            return new LinkedList<>();
        }

        IamInstanceProfileSpecification iamInstanceProfile = new IamInstanceProfileSpecification().
                withName(Utils.MANAGER_I_AM_PROFILE_NAME); // TODO use another iamprofile

        // TODO create a workers security group in AWS
        RunInstancesRequest request = new RunInstancesRequest().
                withImageId(Utils.WORKER_IMAGE_ID).
                withMinCount(n).
                withMaxCount(n).
                withSecurityGroups(Utils.WORKERS_SECURITY_GROUP).
                withInstanceType(Utils.WORKER_INSTANCE_TYPE).
                withSecurityGroups(Utils.WORKERS_SECURITY_GROUP).
                withUserData(getWorkerUserDataScript()).
                withIamInstanceProfile(iamInstanceProfile);
        RunInstancesResult runInstancesResult = ec2.runInstances(request);

        // Add ssh key for development
        if (getAwsKeyName() != null){
            request.setKeyName(getAwsKeyName());
        }

        List<String> instancesIds = runInstancesResult.getReservation().getInstances().stream().map(Instance::getInstanceId).collect(Collectors.toList());

        tagWorkers(instancesIds);
        return instancesIds;
    }

    private static String getAwsKeyName() {
        return System.getenv("AWS_KEY_NAME");
    }

    private static void tagWorkers(List<String> instancesIds) {
        List<Tag> tags = new LinkedList<>();
        tags.add(new Tag("Name", "worker"));
        CreateTagsRequest createTagsRequest = new CreateTagsRequest(instancesIds, tags);
        try {
            ec2.createTags(createTagsRequest);
        }catch (Exception e){
            logger.warn("When trying to tag ec2 workers, retrying ",e);
            try {
                Thread.sleep(20*1000);
            } catch (InterruptedException ignored) {}

            ec2.createTags(createTagsRequest);
        }
    }

    /**
     * Wait for a list of instances
     */
    public static void waitForRunning(List<String> instancesIds){
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
        IamInstanceProfileSpecification iamInstanceProfile = new IamInstanceProfileSpecification().
                withName(Utils.MANAGER_I_AM_PROFILE_NAME);

        RunInstancesRequest request = new RunInstancesRequest().
                withImageId(Utils.MANAGER_IMAGE_ID).
                withMinCount(1).
                withMaxCount(1).
                withSecurityGroups(Utils.MANAGER_SECURITY_GROUP).
                withInstanceType(InstanceType.T2Micro).
                withUserData(getManagerUserDataScript()).
                withIamInstanceProfile(iamInstanceProfile);

        // Add ssh key for development
        if (getAwsKeyName() != null){
            request.setKeyName(getAwsKeyName());
        }

        RunInstancesResult runInstancesResult = ec2.runInstances(request);

        String instancesIds = runInstancesResult.getReservation().getInstances().get(0).getInstanceId();
        tagManager(instancesIds);
        return instancesIds;
    }

    private static String getManagerUserDataScript(){
        ArrayList<String> lines = new ArrayList<>();
        lines.add("#! /bin/bash");
        lines.add("sudo su ec2-user");
        lines.add("cd /home/ec2-user");
        lines.add("curl " + Utils.S3_JARS_BUCKET + "dsp-1-manager-1.0-SNAPSHOT-jar-with-dependencies.jar > /home/ec2-user/manager.jar");
        lines.add("curl " + Utils.S3_JARS_BUCKET + "common-1.0-SNAPSHOT-jar-with-dependencies.jar > /home/ec2-user/common.jar");

        // This line allows us to run the command as non root user in order to
        // user the AWS instance credentials
        lines.add("sudo -u ec2-user -H sh -c 'java -cp \"common.jar:manager.jar:aws-java-sdk-1.10.64/*\" com.bgu.dsp.manager.Main > manager_stdout_stderr.log 2>&1'");
        return new String(Base64.encodeBase64(join(lines, "\n").getBytes()));
    }

    private static String getWorkerUserDataScript(){
        ArrayList<String> lines = new ArrayList<>();
        lines.add("#! /bin/bash");
        lines.add("cd /home/ec2-user");
        lines.add("curl " + Utils.S3_JARS_BUCKET + "dsp-1-worker-1.0-SNAPSHOT-jar-with-dependencies.jar > /home/ec2-user/worker.jar");
        lines.add("curl " + Utils.S3_JARS_BUCKET + "common-1.0-SNAPSHOT-jar-with-dependencies.jar > /home/ec2-user/common.jar");

        lines.add("mkdir external-jars");
        String ejml = "ejml-0.23.jar";
        lines.add("curl " + Utils.S3_JARS_BUCKET + ejml + " > /home/ec2-user/external-jars/" + ejml);
        String jollyday = "jollyday-0.4.7.jar";
        lines.add("curl " + Utils.S3_JARS_BUCKET + jollyday + " > /home/ec2-user/external-jars/" + jollyday);
        String stanfordCoreNlpModels = "stanford-corenlp-3.3.0-models.jar";
        lines.add("curl " + Utils.S3_JARS_BUCKET + stanfordCoreNlpModels + " > /home/ec2-user/external-jars/" + stanfordCoreNlpModels);
        String stanfordCoreNlp = "stanford-corenlp-3.3.0.jar";
        lines.add("curl " + Utils.S3_JARS_BUCKET + stanfordCoreNlp + "  > /home/ec2-user/external-jars/" + stanfordCoreNlp);

        // This line allows us to run the command as non root user in order to
        // user the AWS instance credentials
        String javaCmd = "java -Xmx1000m -Xms1000m -cp \"worker.jar:common.jar:aws-java-sdk-1.10.64/*:external-jars/*\" com.bgu.dsp.worker.Worker > worker_stdout_stderr.log 2>&1";
        lines.add("sudo -u ec2-user -H sh -c '" + javaCmd + "'");
        return new String(Base64.encodeBase64(join(lines, "\n").getBytes()));
    }

    static String join(Collection<String> s, String delimiter) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delimiter);
        }
        return builder.toString();
    }

    private static void tagManager(String instancesId) {
        List<String> instances = new LinkedList<>();
        instances.add(instancesId);

        List<Tag> tags = new LinkedList<>();
        tags.add(new Tag("Name", "manager"));

        CreateTagsRequest createTagsRequest = new CreateTagsRequest(instances, tags);
        ec2.createTags(createTagsRequest);

    }


    public static void terminateAllWorkers() {
        List<String> workesrIDs = new LinkedList<>();

        DescribeInstancesResult allWorkers = getAllWorkers();
        for (Reservation reservation : allWorkers.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                workesrIDs.add(instance.getInstanceId());
            }
        }
        terminateByIds(workesrIDs);
    }

    private static void terminateByIds(List<String> workesrIDs) {
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

	/**
     * Terminates the manager machine
     * If there is no manager ignores the request silently
     */
    public static void terminateManager() {
        Instance managerInstance = getManagerInstance();
        if (managerInstance == null){
            return;
        }
        String instanceId =  managerInstance.getInstanceId();
        List<String> ids = new LinkedList<>();
        ids.add(instanceId);
        terminateByIds(ids);
    }
}
