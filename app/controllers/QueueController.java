package controllers;

import java.io.IOException;

import javax.inject.Inject;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

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

    public static void get() {
		Logger.info("History: %d", queue.getHistory(10).size());
    	ArrayNode array = Nodes.newArray();
    	for (Command command : queue.getHistory(10)) {
    		array.add(CommandSerializer.toJson(command));
    	}
    	renderJson(array);
    }

    public static void post(String id) throws IOException {
		Logger.info("Undo: %s", id);
    	Command cmd = queue.find(id);
    	notFoundIfNull(cmd);
    	String commandId = queue.execute(cmd.reverse());
    	response.status = StatusCode.CREATED; 
        response.setHeader("Undo", String.format("/queue/%s", commandId));
    }

	private static void renderJson(JsonNode object) {
		throw new RenderJackson(object);
	}
}
