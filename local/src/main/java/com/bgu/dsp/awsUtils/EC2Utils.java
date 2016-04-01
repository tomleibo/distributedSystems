package com.bgu.dsp.awsUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;

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
	}

	/**
	 * Count machines who's name matches *worker* and that are either pending or running
	 * @return
	 */
	public static int countWorkers(){
		int instanceCount = 0;
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



		DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);
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

}
