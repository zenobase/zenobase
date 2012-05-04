package com.zenobase.controllers;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.primitives.Ints;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandInfo;
import com.zenobase.common.PartialList;
import com.zenobase.json.Nodes;

public class CommandList extends PartialList<Command> {

	public CommandList(Iterable<Command> elements, long size) {
		super(elements, size);
	}

    public ObjectNode toJson() {
    	ObjectNode node = Nodes.newObject();
    	TOTAL.setValue(node, Ints.checkedCast(size()));
    	ArrayNode commandsNode = node.putArray("commands");
    	for (Command command : getElements()) {
    		commandsNode.add(new CommandInfo(command).toJson());
    	}
    	return node;
    }
}
