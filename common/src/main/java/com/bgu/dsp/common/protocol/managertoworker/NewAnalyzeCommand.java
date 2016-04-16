package com.bgu.dsp.common.protocol.managertoworker;

import com.bgu.dsp.awsUtils.SQSUtils;
import com.bgu.dsp.common.protocol.managertolocal.Tweet;
import com.bgu.dsp.common.protocol.workertomanager.WorkerToManagerSQSProtocol;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.UUID;

public class NewAnalyzeCommand implements ManagerToWorkerCommand {

    final static Logger log = Logger.getLogger(NewAnalyzeCommand.class);

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
	public void execute(TweetAnalyzer analyzer, UUID workerUUID) {
		Tweet tweet;
		try {
			String title= getTitleFromUrl();
			tweet = analyzer.analyze(title, workerUUID);
		} catch (Exception e) {
			log.warn("Got an errornous tweet url " + getTweetUrl(), e);
			tweet = new Tweet(e.getMessage(), workerUUID);
		}
		uploadTweetToQueue(tweet);
	}


    private String getTitleFromUrl() throws IOException {
        Document doc = Jsoup.connect(tweetUrl).get();
		String title = doc.title();
		return title;
    }

    private void uploadTweetToQueue(Tweet tweet) {
        String msg = WorkerToManagerSQSProtocol.newCompletedMessage(tweet);
        String url = SQSUtils.getQueueUrlByName(sqsQueueName);
        SQSUtils.sendMessage(url, msg);
    }
}
