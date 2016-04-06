package com.bgu.dsp.common.protocol.workertomanager;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by hagai_lvi on 05/04/2016.
 */
public class WorkerToManagerMessage implements Serializable{
	private final UUID uuid;
	private final String message;

	public UUID getUuid() {
		return uuid;
	}

	public String getMessage() {
		return message;
	}


	public WorkerToManagerMessage(UUID uuid, String message) {
		this.uuid = uuid;
		this.message = message;
	}

}
