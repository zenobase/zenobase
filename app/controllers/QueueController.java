package controllers;

import javax.inject.Inject;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import play.mvc.Result;
import services.CommandQueue;

import commands.Command;
import commands.CommandSerializer;
import common.Nodes;

public class QueueController extends ControllerSupport {

	@Inject
	static CommandQueue queue;

    public static Result get(int offset, int limit) {
    	ObjectNode object = Nodes.newObject();
    	object.put("total", queue.size());
    	ArrayNode commandsNode = object.putArray("commands");
    	for (Command command : queue.getHistory(offset, limit)) {
    		commandsNode.add(CommandSerializer.toJson(command));
    	}
    	return ok(object);
    }

    public static Result post(String id) {
    	Command command = queue.find(id);
    	if (command == null) {
    		return notFound();
    	}
		if (!command.getIdentity().equals(SecurityController.identity(false))) {
			return forbidden();
		}
        return undo(command);
    }

    private static Result undo(Command command) {
    	String undoId = queue.execute(command.reverse());
    	response().setHeader("Undo",  String.format("/queue/%s", undoId));
        return created();
    }
}
