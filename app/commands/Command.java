package commands;

import models.Identity;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;

public interface Command {

	String getType();

	String getId();

	Identity getPrincipal();

	DateTime getTimestamp();

	Command reverse(Identity principal);

	ObjectNode toJson();
}
