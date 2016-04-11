package com.bgu.dsp.common.protocol.managertolocal.serialize;

/**
 * Thrown when we get an object that was not expected <br>
 * I.E when we expect to deserialize an object of class A and we get object of class B
 *
 */
public class IllegalSerializedObjectException extends Throwable {
	public IllegalSerializedObjectException(String msg) {
		super(msg);
	}
}
