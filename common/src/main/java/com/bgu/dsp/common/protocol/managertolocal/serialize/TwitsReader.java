package com.bgu.dsp.common.protocol.managertolocal.serialize;

import com.bgu.dsp.common.protocol.managertolocal.Tweet;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * deserialize instances of {@link com.bgu.dsp.common.protocol.managertolocal.Tweet} from a file.  <br>
 *
 * Client must call {@link #init()} and {@link #close()} <br>
 *
 * Client must know how many objects are in the file (i.e, when he should stop calling read)
 *
 * @see TwitsWriter
 *
 */
public class TwitsReader {

	private final String filePath;
	private FileInputStream inputStream;
	private ObjectInputStream objectInputStream;

	public  TwitsReader(String filePath) {
		this.filePath = filePath;
	}

	public void init() throws IOException {
		this.inputStream = new FileInputStream(filePath);
		this.objectInputStream = new ObjectInputStream(inputStream);
	}

	public void close() throws IOException {
		this.objectInputStream.close();
		this.inputStream.close();
	}

	/**
	 *
	 * @return
	 * @throws EOFException if reached to the end of the file
	 * @throws IllegalSerializedObjectException if the object read from the stream is not a tweet
	 */
	public Tweet read() throws ClassNotFoundException, IllegalSerializedObjectException, IOException {
		Object o = this.objectInputStream.readObject();
		if (! (o instanceof Tweet)){
			throw new IllegalSerializedObjectException("Expected object of class " + Tweet.class.toString() +
			"but got " + o.getClass().toString());
		}
		return (Tweet) o;
	}

}
