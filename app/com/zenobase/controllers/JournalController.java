package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;

import com.zenobase.commands.Command;
import com.zenobase.models.CommandList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CommandQuery;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;

public class JournalController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final CommandRepository repository;
	private final UserRepository users;

	@Inject
    public JournalController(AuthorizationContext security, CommandDispatcher dispatcher,
    	CommandRepository repository, UserRepository users) {

		super(security);
		this.dispatcher = dispatcher;
		this.repository = repository;
		this.users = users;
	}

	public Result findAll(String q, int offset, int limit) {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	if (auth.getScope() != null) {
    		return forbidden();
    	}
    	if (!users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
    	CommandQuery query = new CommandQuery();
    	if (q != null) {
    		query = query.queryString(q);
    	}
    	return ok(CommandList.toJson(repository.find(query, CommandQuery.DEFAULT_ORDER, offset, limit)));
    }

	public Result findByUser(String userId, int offset, int limit) {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	if (auth.getScope() != null) {
    		return forbidden();
    	}
		Identity principal = new UserLookup(users).getIdentity(userId);
		if (principal == null) {
			return notFound("user not found");
		}
		if (!auth.getPrincipal().equals(principal) && !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
    	return ok(CommandList.toJson(repository.find(new CommandQuery().principalEqualTo(principal), CommandQuery.DEFAULT_ORDER, offset, limit)));
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
