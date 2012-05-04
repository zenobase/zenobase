package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.commands.Command;
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
    	Identity principal = auth.getPrincipal();
    	if (principal == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(principal)) {
    		return forbidden();
    	}
    	return ok(store.getHistory(offset, limit).toJson());
    }

	@BodyParser.Of(BodyParser.Json.class)
    public static Result post() {
    	UndoForm form = new UndoForm(body());
		if (!form.valid()) {
			return badRequest("missing command");
		}
		Identity principal = auth.getPrincipal();
    	if (principal == null) {
    		return unauthorized();
    	}
    	Command command = store.find(form.getCommandId());
    	if (command == null) {
    		return notFound("command not found");
    	}
		if (!principal.equals(command.getPrincipal()) && !users.isSuperuser(principal)) {
			return forbidden();
		}
    	String undoId = queue.dispatch(command.reverse(principal));
        return created(receipt(undoId));
    }
}
