package controllers;

import java.io.IOException;

import javax.inject.Inject;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import services.CommandQueue;

import commands.Command;

@With(UserController.class)
public class QueueController extends Controller {

	@Inject
	static CommandQueue queue;

    public static void post(String id) throws IOException {
		Logger.info("Command: %s", id);
    	Command cmd = queue.find(id);
    	notFoundIfNull(cmd);
    	queue.execute(cmd.reverse());
    	DashboardController.get();
    }
}
