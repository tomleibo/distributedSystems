package com.bgu.dsp.common.protocol.workertomanager;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class WorkerToManagerMessage implements Serializable{
	private final UUID uuid;
	private final String message;
	private final List<Entity> entities;
	private final int sentiment;

	public List<Entity> getEntities() {
		return entities;
	}

	public int getSentiment() {
		return sentiment;
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getMessage() {
		return message;
	}


	public WorkerToManagerMessage(UUID uuid, String message, List<Entity> entities, int sentiment) {
		if (sentiment < 0 || sentiment > 4){
			throw new IllegalArgumentException("Sentiment must be in the range [0,4].\n" +
					"Got " + sentiment);
		}
		this.uuid = uuid;
		this.message = message;
		this.entities = entities;
		this.sentiment = sentiment;
	}

	public static class Entity {
		private final String name, type;

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}

		public Entity(String name, String type) {
			this.name = name;
			this.type = type;
		}
	}



}
