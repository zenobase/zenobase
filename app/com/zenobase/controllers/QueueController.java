package com.zenobase.controllers;

import javax.inject.Inject;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandSupport;
import com.zenobase.common.Nodes;
import com.zenobase.common.PartialList;
import com.zenobase.common.SecurityContext;
import com.zenobase.models.Identity;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.CommandStore;
import com.zenobase.services.UserManager;


@With(Timed.class)
public class QueueController extends ControllerSupport {

	@Inject
	static CommandQueue queue;

	@Inject
	static CommandStore store;

	@Inject
	static UserManager users;

    public static Result get(int offset, int limit) {
    	Identity principal = new SecurityContext(ctx()).getPrincipal();
    	if (principal == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(principal)) {
    		return forbidden();
    	}
    	return ok(toJson(store.getHistory(offset, limit)));
    }

    private static ObjectNode toJson(PartialList<Command> commands) {
    	ObjectNode node = Nodes.newObject();
    	node.put("total", commands.size());
    	ArrayNode commandsNode = node.putArray("commands");
    	for (Command command : commands.getElements()) {
    		ObjectNode commandNode = Nodes.copy(command.toJson());
    		commandNode.put("label", command.toString());
    		commandNode.remove(CommandSupport.PARAMETERS.getName());
    		commandsNode.add(commandNode);
    	}
    	return node;
    }

    public static Result post(String id) {
    	Identity principal = new SecurityContext(ctx()).getPrincipal();
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
