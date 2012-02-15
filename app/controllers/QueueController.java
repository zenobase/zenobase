package controllers;

import java.io.IOException;

import javax.inject.Inject;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http.StatusCode;
import services.CommandQueue;

import commands.Command;
import commands.CommandSerializer;
import common.Nodes;
import common.RenderJackson;

public class QueueController extends Controller {

	@Inject
	static CommandQueue queue;

    public static void get(int offset, int limit) {
		Logger.info("History: %d-%d", offset, limit);
    	ObjectNode object = Nodes.newObject();
    	object.put("total", queue.size());
    	ArrayNode commandsNode = object.putArray("commands");
    	for (Command command : queue.getHistory(offset, limit)) {
    		commandsNode.add(CommandSerializer.toJson(command));
    	}
    	renderJson(object);
    }

    public static void post(String id) throws IOException {
		Logger.info("Undo: %s", id);
    	Command cmd = queue.find(id);
    	notFoundIfNull(cmd);
		String user = AuthController.currentUser();
		if (!user.equals(cmd.getUser())) {
			forbidden();
		}
    	String commandId = queue.execute(cmd.reverse());
    	response.status = StatusCode.CREATED; 
        response.setHeader("Undo", String.format("/queue/%s", commandId));
    }

	private static void renderJson(JsonNode object) {
		throw new RenderJackson(object);
	}
}
