package commands;

import org.joda.time.DateTime;

import models.Identity;

public interface Command {

	void execute();

	Command reverse(Identity identity);

	String getId();

	Identity getIdentity();

	DateTime getTimestamp();
}
