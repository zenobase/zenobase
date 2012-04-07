package commands;

import org.joda.time.DateTime;

import secure.Identity;

import common.Generator;

public abstract class CommandSupport implements Command {

	private final String id = Generator.id();
	private final Identity identity;
	private final DateTime timestamp = new DateTime();

	public CommandSupport(Identity identity) {
		this.identity = identity;
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
