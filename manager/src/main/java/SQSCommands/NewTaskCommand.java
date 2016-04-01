package SQSCommands;

import com.amazonaws.util.IOUtils;
import com.bgu.dsp.awsUtils.S3Utils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hagai_lvi on 30/03/2016.
 */
public class NewTaskCommand implements SQSCommand {

	final static Logger logger = Logger.getLogger(NewTaskCommand.class);
	private final double linesPerWorker = 100.0;//TODO
	private final String bucketName;
	private final String key;

	public NewTaskCommand(String bucketName, String key){
		this.bucketName = bucketName;
		this.key = key;
	}
	public void execute() {
		String fileContent = getFileContent();

		double numOfWorkers = getNumOfWorkers(fileContent);

		logger.info("Using " + numOfWorkers + " workers overall");
	}

	private double getNumOfWorkers(String fileContent) {
		int numberOfLines = countLines(fileContent);
		return Math.ceil(numberOfLines / linesPerWorker);
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
