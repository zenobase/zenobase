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
    	Identity principal = Identities.in(ctx()).get();
    	if (principal == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(principal)) {
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
    	Identity principal = Identities.in(ctx()).get();
    	if (principal == null) {
    		return unauthorized();
    	}
    	Command command = store.find(id);
    	if (command == null) {
    		return notFound();
    	}
		if (!principal.equals(command.getPrincipal()) && !users.isSuperuser(principal)) {
			return forbidden();
		}
        return undo(command, principal);
    }

    private static Result undo(Command command, Identity principal) {
    	String undoId = queue.dispatch(command.reverse(principal));
    	response().setHeader("Undo",  String.format("/queue/%s", undoId));
        return created();
    }
}
