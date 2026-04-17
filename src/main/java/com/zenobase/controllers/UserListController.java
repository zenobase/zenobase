package com.zenobase.controllers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.io.UserPrinter;
import com.zenobase.models.UserList;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.UserQuery;
import com.zenobase.repositories.UserRepository;

public class UserListController extends ControllerSupport {

	private static final Logger logger = LoggerFactory.getLogger(UserListController.class);

	private final UserRepository repository;

	@Inject
	public UserListController(AuthorizationContext security, UserRepository repository) {
		super(security);
		this.repository = repository;
	}

	public void find(ServerRequest req, ServerResponse res) {
		String q = req.query().first("q").orElse(null);
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));

		Authorization auth = getCurrentAuthorization(req);
		if (auth == null || auth.getScope() != null) {
			sendUnauthorized(res);
			return;
		}
		if (!repository.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		if (limit == Integer.MAX_VALUE) {
			findAll(res);
			return;
		}
		UserQuery query = new UserQuery();
		if (q != null) {
			query = query.queryString(q);
		}
		sendOk(res, UserList.toJson(repository.find(query, offset, limit)));
	}

	private void findAll(ServerResponse res) {
		setHeader(res, "Content-Type", "text/plain");
		try (OutputStream os = res.outputStream()) {
			Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
			repository.find(new UserPrinter(writer));
			writer.flush();
		} catch (IOException e) {
			logger.warn("Error streaming users", e);
		}
	}
}
