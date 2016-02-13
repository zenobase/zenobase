package com.zenobase.commands;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import play.Logger;

public class CommandParserRegistry {

	private final Map<String, CommandParser> parsers = Maps.newHashMap();

	@Inject
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
		CommandParser parser = parsers.get(type.getName());
		Preconditions.checkNotNull(parser, "Missing parser for type '%s'", type.getName());
		renameUser("thomaghenry", "thomasghenry", node);
		Command command = parser.parse(node, type.getVersion());
		Preconditions.checkNotNull(command, "Missing parser for version %s of type '%s'", type.getVersion(), type.getName());
		return command;
	}

	private static void renameUser(String from, String to, ObjectNode node) {
		if (from.equals(node.path("parameters").path("username").textValue())) {
			((ObjectNode) node.path("parameters")).put("username", to);
			Logger.warn("updated the username", node);
		} else if (from.equals(node.path("parameters").path("user").path("name").textValue())) {
			((ObjectNode) node.path("parameters").path("user")).put("name", to);
			Logger.warn("updated the user.name", node);
		}
	}
}
