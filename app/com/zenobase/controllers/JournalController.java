package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;
import com.google.common.base.Strings;

import com.zenobase.actions.Timed;
import com.zenobase.commands.Command;
import com.zenobase.models.Identity;
import com.zenobase.models.CommandList;
import com.zenobase.oauth.Authorization;
import com.zenobase.search.QueryConstraint;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.UserRepository;

@With(Timed.class)
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

	public Result get(String query, int offset, int limit) {
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
		QueryConstraint constraint = null;
		if (!Strings.isNullOrEmpty(query)) {
			try {
				constraint = QueryConstraint.parse(query);
			} catch (IllegalArgumentException e) {
				return badRequest("query is malformed");
			}
		}
		if (constraint == null || !isConstrainedToPrincipal(constraint, auth.getPrincipal())) {
			if (users.isSuperuser(auth.getPrincipal())) {
		    	return ok(CommandList.toJson(repository.find(offset, limit, true)));
			}
			return forbidden();
		}
    	return ok(CommandList.toJson(repository.find(constraint.getField(), constraint.getValue(), offset, limit, true)));
    }

	private static boolean isConstrainedToPrincipal(QueryConstraint constraint, Identity principal) {
		return Command.PRINCIPAL.getName().equals(constraint.getField())
			&& principal.getId().equals(constraint.getValue());
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
