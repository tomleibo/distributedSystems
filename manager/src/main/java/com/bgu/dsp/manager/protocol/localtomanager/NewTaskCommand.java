package com.bgu.dsp.manager.protocol.localtomanager;

import com.amazonaws.util.IOUtils;
import com.bgu.dsp.awsUtils.EC2Utils;
import com.bgu.dsp.awsUtils.S3Utils;
import com.bgu.dsp.awsUtils.SQSUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hagai_lvi on 30/03/2016.
 */
public class NewTaskCommand implements LocalToManagerCommand {

	final static Logger logger = Logger.getLogger(NewTaskCommand.class);
	private static final String WORKERS_QUEUE_RUL = "";
	public static final String MANAGER_WORKERS_QUEUE_NAME = "manager-workers-queue";
	private final double linesPerWorker = 100.0;//TODO
	private final String bucketName;
	private final String key;

	public NewTaskCommand(String bucketName, String key){
		this.bucketName = bucketName;
		this.key = key;
	}
	public void execute() {
		String fileContent = getFileContent();
		startWorkers(fileContent);
		postTweetsToQueue(fileContent);
	}

	private void postTweetsToQueue(String fileContent) {

		// TODO what happens if the queue already exist?!
		String queueUrl = SQSUtils.createQueue(MANAGER_WORKERS_QUEUE_NAME);

		BufferedReader bufReader = new BufferedReader(new StringReader(fileContent));

		String line;

		try {
			while( (line = bufReader.readLine()) != null ) {
				SQSUtils.sendMessage(queueUrl, line);
			}
		} catch (IOException e) {
			logger.warn(e);
		}

	}

	private void startWorkers(String fileContent) {
		int numOfWorkers = getNumOfWorkers(fileContent);
		int currentNumOfWorkers = EC2Utils.countWorkers();
		int workersToStart = numOfWorkers - currentNumOfWorkers;
		logger.info(currentNumOfWorkers + " workers are running\n" +
				"Starting " + workersToStart + " more workers for total of " + numOfWorkers + " workers.");
		// TODO start the workers
	}

	private int getNumOfWorkers(String fileContent) {
		int numberOfLines = countLines(fileContent);
		return (int)Math.ceil(numberOfLines / linesPerWorker);
	}

	private String getFileContent() {
		// TODO what if the file is larger then the memory? can we save it in chunks?
		InputStream fileInputStream = S3Utils.getFileInputStream(bucketName, key);
		String fileContent;
		try {
			fileContent = IOUtils.toString(fileInputStream);
		} catch (IOException e) {
			// TODO how to handle IOException?
			throw new RuntimeException(e);
		}
		finally {
			try {
				fileInputStream.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return fileContent;
	}

	private int countLines(String fileContent) {
		Matcher m = Pattern.compile("\r\n|\r|\n").matcher(fileContent);
		int lines = 1;
		while (m.find()) {
			lines ++;
		}
		return lines;
	}
}
