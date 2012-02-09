package commands;

import org.joda.time.DateTime;

import common.Generator;

public abstract class CommandSupport implements Command {

	private final String id = Generator.id();
	private final String user;
	private final DateTime timestamp = new DateTime();

	public CommandSupport(String user) {
		this.user = user;
	}

	public String getId() {
		return id;
	}
	
	public String getUser() {
		return user;
	}

	public DateTime getTimestamp() {
		return timestamp;
	}
}
