package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;

import com.zenobase.models.Identity;

public interface Command {

	String getType();

	String getId();

	Identity getPrincipal();

	DateTime getTimestamp();

	Command reverse(Identity principal);

	ObjectNode toJson();
}
