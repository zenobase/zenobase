package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;

public interface CommandParser {

	String getTypeName();

	Command parse(ObjectNode node, int version);

	void registered(CommandParserRegistry registry);
}
