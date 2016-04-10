package com.bgu.dsp.common.protocol.managertoworker;

import java.util.UUID;

public class NewAnalyzeCommand implements ManagerToWorkerCommand {

	public UUID getUuid() {
		return uuid;
	}

	public String getSqsQueueName() {
		return sqsQueueName;
	}

	public String getTweetUrl() {
		return tweetUrl;
	}

	private final UUID uuid;
	private final String sqsQueueName;
	private final String tweetUrl;

	@Override
	public String toString() {
		return "NewAnalyzeCommand{" +
				"uuid=" + uuid +
				", sqsQueueName='" + sqsQueueName + '\'' +
				", tweetUrl='" + tweetUrl + '\'' +
				'}';
	}

	public NewAnalyzeCommand(UUID uuid, String sqsQueueName, String tweetUrl) {
		this.uuid = uuid;
		this.sqsQueueName = sqsQueueName;
		this.tweetUrl = tweetUrl;
	}

	@Override
	public void execute() {
		// TODO implement by the workers
		System.out.println(toString());
	}
}
