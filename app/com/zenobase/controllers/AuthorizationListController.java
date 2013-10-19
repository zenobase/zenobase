package com.zenobase.controllers;

import javax.inject.Inject;

import org.joda.time.Period;
import play.mvc.Result;
import com.google.common.base.Strings;

import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.DeleteAuthorizationCommand;
import com.zenobase.common.PartialList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.oauth.AuthorizationList;
import com.zenobase.search.QueryConstraint;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

public class AuthorizationListController extends ControllerSupport {

	private final AuthorizationRepository authorizations;
	private final CommandDispatcher dispatcher;
	private final UserRepository users;

	@Inject
	public AuthorizationListController(AuthorizationContext security, AuthorizationRepository authorizations, CommandDispatcher dispatcher,UserRepository users) {
		super(security);
		this.authorizations = authorizations;
		this.dispatcher = dispatcher;
		this.users = users;
	}

	public Result find(String query, boolean clientOnly, int offset, int limit) {
		if (limit > 100) {
			return badRequest("limit can't be more than 100");
		}
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	if (auth.getScope() != null) {
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
		if (!isConstrainedToPrincipal(constraint, auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		return constraint != null
			? ok(AuthorizationList.toJson(authorizations.find(constraint.getField(), constraint.getValue(), clientOnly, offset, limit)))
			: ok(AuthorizationList.toJson(authorizations.find(offset, limit)));
    }

	private static boolean isConstrainedToPrincipal(QueryConstraint constraint, Identity principal) {
		return constraint != null
			&& Authorization.PRINCIPAL.getName().equals(constraint.getField())
			&& principal.getId().equals(constraint.getValue());
	}

	public Result delete() {
		Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
		return delete(Period.months(1), auth.getPrincipal());
	}

	public Result delete(Period maxAge, Identity principal) {
		PartialList<Authorization> results = authorizations.find(maxAge, 0, 1000);
		CompoundCommand command = new CompoundCommand(principal,
			String.format("removed %d authorizations(s)", results.size()),
			String.format("restored %d authorizations(s)", results.size()));
    	for (Authorization result : results) {
    		command.add(new DeleteAuthorizationCommand(principal, result));
    	}
		String commandId = dispatcher.dispatch(command);
		response().setHeader(COMMAND_ID, commandId);
		return noContent();
	}
}
