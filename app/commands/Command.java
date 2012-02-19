package commands;

import org.joda.time.DateTime;

import secure.Identity;

public interface Command {

	void execute();

	Command reverse();

	String getId();

	Identity getIdentity();

	DateTime getTimestamp();
}
