package commands;

import org.joda.time.DateTime;

import models.Identity;

public interface Command {

	String getType();

	String getId();

	Identity getIdentity();

	DateTime getTimestamp();

	Command reverse(Identity identity);
}
