package controllers;

import javax.inject.Inject;

import models.Identity;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import play.mvc.Result;
import play.mvc.With;
import services.CommandQueue;
import services.CommandStore;
import services.UserManager;

import commands.Command;
import commands.CommandSupport;
import common.Identities;
import common.Nodes;

@With(Timed.class)
public class QueueController extends ControllerSupport {

	@Inject
	static CommandQueue queue;

	@Inject
	static CommandStore store;

	@Inject
	static UserManager users;

    public static Result get(int offset, int limit) {
    	Identity identity = Identities.in(ctx()).get();
    	if (identity == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(identity)) {
    		return forbidden();
    	}
    	ObjectNode object = Nodes.newObject();
    	object.put("total", store.size());
    	ArrayNode commandsNode = object.putArray("commands");
    	for (Command command : store.getHistory(offset, limit)) {
    		ObjectNode commandNode = Nodes.copy(command.toJson());
    		commandNode.put("label", command.toString());
    		commandNode.remove(CommandSupport.PARAMETERS.getName());
    		commandsNode.add(commandNode);
    	}
    	return ok(object);
    }

    public static Result post(String id) {
    	Identity identity = Identities.in(ctx()).get();
    	if (identity == null) {
    		return unauthorized();
    	}
    	Command command = store.find(id);
    	if (command == null) {
    		return notFound();
    	}
		if (!identity.equals(command.getIdentity()) && !users.isSuperuser(identity)) {
			return forbidden();
		}
        return undo(command, identity);
    }

    private static Result undo(Command command, Identity identity) {
    	String undoId = queue.dispatch(command.reverse(identity));
    	response().setHeader("Undo",  String.format("/queue/%s", undoId));
        return created();
    }
}
