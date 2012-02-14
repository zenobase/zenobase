package commands;

import org.codehaus.jackson.node.ObjectNode;

import common.Nodes;

public class CommandSerializer {

	public CommandSerializer() {
		
	}

	public static ObjectNode toJson(Command command) {
		ObjectNode object = Nodes.newObject();
		object.put("id", command.getId());
		object.put("label", command.toString());
		object.put("user", command.getUser());
		object.put("timestamp", command.getTimestamp().toString());
		return object;
	}
}
