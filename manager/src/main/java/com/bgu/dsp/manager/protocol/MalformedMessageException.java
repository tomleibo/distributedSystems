package com.bgu.dsp.manager.protocol;

/**
 * Created by hagai_lvi on 30/03/2016.
 */
public class MalformedMessageException extends Exception {

	public MalformedMessageException(String message) {
		super(message);
	}

	public MalformedMessageException(String message, Throwable cause) {
		super(message, cause);
	}
}
