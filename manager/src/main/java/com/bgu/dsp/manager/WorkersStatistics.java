package com.bgu.dsp.manager;

import com.bgu.dsp.common.WorkersStatisticsI;

import java.util.HashMap;
import java.util.UUID;

public class WorkersStatistics implements WorkersStatisticsI {
	private HashMap<UUID, Stats> map = new HashMap<>();

	@Override
	public void addSuccessfulTask(UUID workerUUID) {
		Stats current = map.getOrDefault(workerUUID, new Stats());
		current.addSuccessfulTask();
		map.put(workerUUID, current);
	}

	@Override
	public String toString() {

		String res = "";

		for (UUID uuid : map.keySet()) {
			res += uuid + ":" + map.get(uuid) + "\n";
		}
		return "WorkersStatistics{\n" +
				res +
				'}';
	}

	@Override
	public void addFaultyTask(UUID workerUUID) {
		Stats current = map.getOrDefault(workerUUID, new Stats());
		current.addFaultyTask();
		map.put(workerUUID, current);
	}

	private class Stats{

		@Override
		public String toString() {
			return "successCount=" + successCount +
					", failCount=" + failCount +
					", totalCount=" + (successCount + failCount);
		}

		private long successCount = 0;
		private long failCount = 0;

		public long getFailCount() {
			return failCount;
		}

		public long getSuccessCount() {
			return successCount;
		}

		public void addSuccessfulTask() {
			successCount++;
		}

		public void addFaultyTask(){
			failCount++;
		}
	}
}
