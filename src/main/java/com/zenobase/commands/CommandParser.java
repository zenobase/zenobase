package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

public abstract class CommandParser {

	private @Nullable CommandParserRegistry registry;

	public abstract String getTypeName();

	public void registered(CommandParserRegistry registry) {
		this.registry = registry;
	}

	protected @Nullable CommandParserRegistry getRegistry() {
		return registry;
	}

	public abstract @Nullable Command parse(ObjectNode node, int version);
}
