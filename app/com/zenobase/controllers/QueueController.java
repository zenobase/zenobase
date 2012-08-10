package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.commands.Command;
import com.zenobase.models.CommandList;
import com.zenobase.models.Identity;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class QueueController extends ControllerSupport {

	@Inject
	static CommandDispatcher dispatcher;

	@Inject
	static CommandRepository repository;

	@Inject
	static UserRepository users;

    public static Result get(String identity, int offset, int limit) {
    	Identity principal = auth.getPrincipal();
    	if (principal == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(principal)) {
    		return forbidden();
    	}
    	CommandList commands = identity != null ?
    		repository.find(new Identity(identity), offset, limit, true) :
    		repository.findAll(offset, limit, true);
    	return ok(commands.toJson());
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
    	Command command = repository.find(form.getCommandId());
    	if (command == null) {
    		return notFound("command not found");
    	}
		if (!principal.equals(command.getPrincipal()) && !users.isSuperuser(principal)) {
			return forbidden();
		}
    	String undoId = dispatcher.dispatch(command.reverse(principal));
        return created(receipt(undoId));
    }
}
