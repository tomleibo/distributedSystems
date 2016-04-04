package SQSCommands;

import com.bgu.dsp.awsUtils.S3Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.bgu.dsp.common.protocol.localtomanager.NewTaskCommand;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class NewTaskCommandTest {

	private String bucketName, key;

	@Before
	public void setUp() throws Exception {
		this.bucketName = "bucket-" + this.getClass().getName().toLowerCase();
		this.key = "KEY_" + this.getClass();

		S3Utils.createBucket(this.bucketName);

		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0 ; i < 1000 ; i++){
			stringBuilder.append("Hello" + i + "\n");
		}

		String msg = stringBuilder.toString();
		byte[] message = msg.getBytes();
		InputStream stream = new ByteArrayInputStream(message);

		S3Utils.uploadFile(this.bucketName, this.key, stream, message.length);
	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void execute() throws Exception {
		NewTaskCommand c = new NewTaskCommand("" // TODO
				, bucketName, key);
		c.run();
	}
}