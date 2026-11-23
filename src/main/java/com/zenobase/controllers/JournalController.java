package com.zenobase.controllers;

import com.zenobase.commands.Command;
import com.zenobase.models.CommandList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.CommandQuery;
import com.zenobase.repositories.CommandRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserLookup;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import java.util.Objects;

public class JournalController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final CommandRepository repository;
	private final UserRepository users;

	@Inject
	public JournalController(
		AuthorizationContext security,
		CommandDispatcher dispatcher,
		CommandRepository repository,
		UserRepository users
	) {
		super(security);
		this.dispatcher = dispatcher;
		this.repository = repository;
		this.users = users;
	}

	public void findAll(ServerRequest req, ServerResponse res) {
		String q = req.query().first("q").orElse(null);
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));
		Authorization auth = requireAuthorization(req);
		requireFirstPartySuperuser(users, auth);
		CommandQuery query = new CommandQuery();
		if (q != null) {
			query = query.queryString(q);
		}
		sendOk(res, CommandList.toJson(repository.find(query, CommandQuery.DEFAULT_ORDER, offset, limit)));
	}

	public void findByUser(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));
		Authorization auth = requireAuthorization(req);
		requireFirstParty(auth);
		Identity principal = requireFound(new UserLookup(users).getIdentity(userId), "user not found");
		requireSelfOrSuperuser(users, auth, principal);
		sendOk(
			res,
			CommandList.toJson(
				repository.find(
					new CommandQuery().principalEqualTo(principal),
					CommandQuery.DEFAULT_ORDER,
					offset,
					limit
				)
			)
		);
	}

	public void post(ServerRequest req, ServerResponse res) {
		UndoForm form = new UndoForm(body(req));
		if (!form.valid()) {
			sendBadRequest(res, "missing command");
			return;
		}
		Authorization auth = requireAuthorization(req);
		Command command = requireFound(
			repository.find(Objects.requireNonNull(form.getCommandId())),
			"command not found"
		);
		requirePermission(command.isPermitted(auth) || isFirstPartySuperuser(users, auth));
		requireNotSuspended(auth);
		String commandId = dispatcher.dispatch(command.reverse(auth.getPrincipal()));
		setHeader(res, COMMAND_ID, commandId);
		sendNoContent(res);
	}
}
