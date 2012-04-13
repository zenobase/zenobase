package commands;

import org.joda.time.DateTime;

import models.Identity;

import common.Generator;

public abstract class CommandSupport implements Command {

	private final String type;
	private final String id = Generator.id();
	private final Identity identity;
	private final DateTime timestamp = new DateTime();

	public CommandSupport(String type, Identity identity) {
		this.type = type;
		this.identity = identity;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Identity getIdentity() {
		return identity;
	}

	@Override
	public DateTime getTimestamp() {
		return timestamp;
	}
}
