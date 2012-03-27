package controllers;

import javax.inject.Inject;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import play.mvc.Result;
import play.mvc.With;
import secure.Identity;
import secure.IdentityHelper;
import secure.UserManager;
import services.CommandQueue;

import commands.Command;
import commands.CommandSerializer;
import common.Nodes;

@With(Timed.class)
public class QueueController extends ControllerSupport {

	@Inject
	static CommandQueue queue;

	@Inject
	static UserManager users;

    public static Result get(int offset, int limit) {
    	// TODO: forbidden() unless superuser
    	ObjectNode object = Nodes.newObject();
    	object.put("total", queue.size());
    	ArrayNode commandsNode = object.putArray("commands");
    	for (Command command : queue.getHistory(offset, limit)) {
    		commandsNode.add(CommandSerializer.toJson(command));
    	}
    	return ok(object);
    }

    public static Result post(String id) {
    	Identity identity = IdentityHelper.in(ctx()).get();
    	Command command = queue.find(id);
    	if (id == null) {
    		return unauthorized();
    	}
    	if (command == null) {
    		return notFound();
    	}
		if (!identity.equals(command.getIdentity()) && !users.isSuperuser(identity)) {
			return forbidden();
		}
        return undo(command, identity);
    }

    private static Result undo(Command command, Identity identity) {
    	String undoId = queue.execute(command.reverse(identity));
    	response().setHeader("Undo",  String.format("/queue/%s", undoId));
        return created();
    }
}
