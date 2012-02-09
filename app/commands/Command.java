package commands;

import org.joda.time.DateTime;

public interface Command {

	void execute();

	Command reverse();

	String getId();

	String getUser();

	DateTime getTimestamp();
}
