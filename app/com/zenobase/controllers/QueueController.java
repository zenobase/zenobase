package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.commands.Command;
import com.zenobase.models.CommandList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class QueueController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final CommandRepository repository;
	private final UserRepository users;

	@Inject
    public QueueController(AuthorizationContext security, CommandDispatcher dispatcher,
    	CommandRepository repository, UserRepository users) {

		super(security);
		this.dispatcher = dispatcher;
		this.repository = repository;
		this.users = users;
	}

	public Result get(String identity, int offset, int limit) {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null || auth.getScope() != null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
    	CommandList commands = identity != null ?
    		repository.find(new Identity(identity), offset, limit, true) :
    		repository.findAll(offset, limit, true);
    	return ok(commands.toJson());
    }

	@BodyParser.Of(BodyParser.Json.class)
    public Result post() {
    	UndoForm form = new UndoForm(body());
		if (!form.valid()) {
			return badRequest("missing command");
		}
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	Command command = repository.find(form.getCommandId());
    	if (command == null) {
    		return notFound("command not found");
    	}
		if (!command.isPermitted(auth) && !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
    	String commandId = dispatcher.dispatch(command.reverse(auth.getPrincipal()));
		response().setHeader(COMMAND_ID, commandId);
        return noContent();
    }
}
