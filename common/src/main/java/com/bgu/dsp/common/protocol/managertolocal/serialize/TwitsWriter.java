package com.bgu.dsp.common.protocol.managertolocal.serialize;

import com.bgu.dsp.common.protocol.managertolocal.Tweet;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Serialize and instances of {@link com.bgu.dsp.common.protocol.managertolocal.Tweet} to a file <br>
 *
 * Client must call {@link #init()} and {@link #close()} <br>
 *
 * Client must count the number of objects written to the file and send them to the reader
 *
 * @see TwitsReader
 */
public class TwitsWriter {

	public static final String FILE = "/Users/hagai_lvi/twits.txt";
	private final String filePath;
	private FileOutputStream outputStream;
	private ObjectOutputStream objectOutputStream;

	public TwitsWriter(String filePath){
		this.filePath = filePath;
	}

	public void init() throws IOException {
		this.outputStream = new FileOutputStream(this.filePath);
		this.objectOutputStream = new ObjectOutputStream(outputStream);
	}

	public void write(Tweet t) throws IOException {
		this.objectOutputStream.writeObject(t);
	}

	public void close() throws IOException {
		this.objectOutputStream.flush();
		this.outputStream.flush();
		this.objectOutputStream.close();
		this.outputStream.close();
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalSerializedObjectException {
		TwitsWriter twitsWriter = new TwitsWriter(FILE);
		twitsWriter.init();
		int n_objects = 10;
		for (int i = 0; i < n_objects; i++){
			LinkedList<String> entities = new LinkedList<>();
			entities.add("entity " + i);
			twitsWriter.write(new Tweet("tweeet" + i, entities,2, UUID.randomUUID()));
		}
		twitsWriter.close();

		TwitsReader reader = new TwitsReader(FILE);
		reader.init();
		for (int i = 0; i < n_objects; i++) {
			Tweet read = reader.read();
			System.out.println(read);
		}
		reader.close();
	}
}
