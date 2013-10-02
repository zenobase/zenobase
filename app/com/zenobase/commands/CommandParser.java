package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class CommandParser {

	private CommandParserRegistry registry;

	public abstract String getTypeName();

	public void registered(CommandParserRegistry registry) {
		this.registry = registry;
	}

	protected CommandParserRegistry getRegistry() {
		return registry;
	}

	public abstract Command parse(ObjectNode node, int version);
}
