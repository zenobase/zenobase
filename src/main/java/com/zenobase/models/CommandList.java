package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.Ints;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandInfo;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.common.PartialList;
import com.zenobase.json.LazyList;
import com.zenobase.json.Nodes;

public class CommandList extends LazyList<Command> {

	private final CommandParserRegistry parsers;

	public CommandList(PartialList<ObjectNode> nodes, CommandParserRegistry parsers) {
		super(nodes);
		this.parsers = parsers;
	}

	@Override
	protected Command toObject(ObjectNode node) {
		return parsers.parse(node);
	}

	public static ObjectNode toJson(PartialList<Command> commands) {
		ObjectNode node = Nodes.newObject();
		TOTAL.setValue(node, Ints.checkedCast(commands.getTotal()));
		ArrayNode commandsNode = node.putArray("commands");
		for (Command command : commands) {
			commandsNode.add(new CommandInfo(command).toJson());
		}
		return node;
	}
}
