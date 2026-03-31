package com.zenobase.commands;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public class CommandParserRegistry {

	private final Map<String, CommandParser> parsers = Maps.newHashMap();

	public CommandParserRegistry(Set<CommandParser> parsers) {
		for (CommandParser parser : parsers) {
			this.parsers.put(parser.getTypeName(), parser);
			parser.registered(this);
		}
	}

	public static CommandParserRegistry containing(CommandParser... parsers) {
		return new CommandParserRegistry(ImmutableSet.copyOf(parsers));
	}

	public Command parse(ObjectNode node) {
		Command.Type type = Command.TYPE.getValue(node);
		Preconditions.checkNotNull(type, "Missing type field in %s", node);
		CommandParser parser = parsers.get(type.name());
		Preconditions.checkNotNull(parser, "Missing parser for type '%s'", type.name());
		Command command = parser.parse(node, type.version());
		Preconditions.checkNotNull(command, "Missing parser for version %s of type '%s'", type.version(), type.name());
		return command;
	}
}
