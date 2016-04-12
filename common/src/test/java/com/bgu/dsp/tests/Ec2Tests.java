package java.com.bgu.dsp.tests;

import com.amazonaws.services.ec2.model.InstanceState;
import com.bgu.dsp.awsUtils.EC2Utils;
import com.bgu.dsp.awsUtils.exceptions.NoSuchInstanceException;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
/**
 * Created by hagai_lvi on 09/04/2016.
 */
public class Ec2Tests {

	@After
	public void tearDown(){
		EC2Utils.terminateAllWorkers();
		EC2Utils.terminateManager();
	}

	@Test
	public void startWorkers() throws NoSuchInstanceException {
		final int NUM_OF_WORKERS = 2;
		EC2Utils.startWorkersAndWait(NUM_OF_WORKERS);

		List<String> allWorkersIds = EC2Utils.getAllWorkersIds();

		assertEquals(NUM_OF_WORKERS, allWorkersIds.size());
		for (String id : allWorkersIds) {
			InstanceState instanceState = EC2Utils.getInstanceState(id);
			assertEquals(instanceState.getName(),"running");
		}

	}

	@Test
	public void startManager() throws NoSuchInstanceException {
		String managerID = EC2Utils.startManager();

		String instanceState = EC2Utils.getInstanceState(managerID).getName();
		assertTrue(instanceState.equals("running") || instanceState.equals("pending"));

		repetitiveAssert(managerID);
	}

	private void repetitiveAssert(String managerID) throws NoSuchInstanceException {
		long start = System.currentTimeMillis();
		long THRESHOLD = 1000 * 60;

		do {
			try {
				assertEquals("running", EC2Utils.getInstanceState(managerID).getName());
				return;
			}
			catch (AssertionError e){
				try {
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e2) {
					e.printStackTrace();
				}
			}
		}while (System.currentTimeMillis() - start < THRESHOLD);

		assertEquals("running", EC2Utils.getInstanceState(managerID).getName());
	}

}
