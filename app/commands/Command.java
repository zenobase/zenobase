package commands;

import models.Identity;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;

public interface Command {

	String getType();

	String getId();

	Identity getIdentity();

	DateTime getTimestamp();

	Command reverse(Identity identity);

	ObjectNode toJson();
}
