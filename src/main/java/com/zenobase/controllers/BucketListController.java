package com.zenobase.controllers;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

import com.zenobase.commands.CreateBucketCommand;
import com.zenobase.common.PartialList;
import com.zenobase.models.Bucket;
import com.zenobase.models.BucketList;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketQuery;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.EventRepository;
import com.zenobase.services.SearchOrder;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;

public class BucketListController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final BucketRepository buckets;
	private final EventRepository events;
	private final UserRepository users;

	@Inject
	public BucketListController(
			AuthorizationContext auth,
			CommandDispatcher dispatcher,
			BucketRepository buckets,
			EventRepository events,
			UserRepository users) {

		super(auth);
		this.dispatcher = dispatcher;
		this.buckets = buckets;
		this.events = events;
		this.users = users;
	}

	public void findAll(ServerRequest req, ServerResponse res) {
		String q = req.query().first("q").orElse(null);
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return;
		}
		if (!users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		if (limit == Integer.MAX_VALUE) {
			findAllStreaming(res);
			return;
		}
		BucketQuery query = new BucketQuery();
		if (q != null) {
			query = query.queryString(q);
		}
		sendOk(res, BucketList.toJson(buckets.find(query, BucketQuery.DEFAULT_ORDER, offset, limit), events));
	}

	private void findAllStreaming(ServerResponse res) {
		setHeader(res, "Content-Type", "text/plain");
		try (var writer = new OutputStreamWriter(res.outputStream(), StandardCharsets.UTF_8)) {
			buckets.findAll(bucket -> {
				try {
					writer.write(toString(bucket));
				} catch (java.io.IOException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String toString(Bucket bucket) {
		return Joiner.on('\t')
				.join(
						bucket.getId(),
						Iterables.getOnlyElement(bucket.getPrincipals(Role.OWNER)),
						bucket.hasRole(new Authorization(Identity.PUBLIC), Role.VIEWER) ? "published" : "unpublished",
						bucket.getCreated(),
						events.size(bucket.getId()),
						"\n");
	}

	public void findByUser(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		String order = req.query().first("order").orElse(null);
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));
		boolean labelsOnly =
				Boolean.parseBoolean(req.query().first("labelsOnly").orElse("false"));
		boolean includeArchived =
				Boolean.parseBoolean(req.query().first("includeArchived").orElse("false"));
		if (offset < 0 || offset > 1000) {
			sendBadRequest(res, "expected offset in [0..1000]");
			return;
		}
		if (limit < 0 || limit > 100) {
			sendBadRequest(res, "expected limit in [0..100]");
			return;
		}
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return;
		}
		Identity principal = new UserLookup(users).getIdentity(userId);
		if (principal == null) {
			sendNotFound(res, "user not found");
			return;
		}
		if (!auth.getPrincipal().equals(principal) && !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		var query = new BucketQuery().principalEqualTo(principal).includeArchived(includeArchived);
		var orderBy = order != null ? SearchOrder.valueOf(order, Bucket.SCHEMA) : BucketQuery.DEFAULT_ORDER;
		PartialList<Bucket> found = buckets.find(query, orderBy, offset, limit);
		sendOk(res, labelsOnly ? BucketList.toJsonLabelsOnly(found) : BucketList.toJson(found, events));
	}

	public void post(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null || auth.getScope() != null) {
			sendUnauthorized(res);
			return;
		}
		var form = new CreateBucketForm(body(req));
		Bucket bucket = form.hasId() ? new Bucket(form.getId()) : new Bucket();
		bucket.setLabel(form.getLabel());
		bucket.setDescription(form.getDescription());
		bucket.setWidgets(form.getWidgets());
		bucket.setAliases(form.getIncluded());
		bucket.addRole(auth.getPrincipal(), Role.OWNER);
		if (!bucket.valid()) {
			sendBadRequest(res, "not valid");
			return;
		}
		if (!new AliasValidator(buckets).checkPermissions(bucket, auth)) {
			sendBadRequest(res, "one or more aliases are invalid");
			return;
		}
		String commandId = dispatcher.dispatch(new CreateBucketCommand(auth.getPrincipal(), bucket));
		setHeader(res, LOCATION, "/buckets/" + bucket.getId());
		setHeader(res, COMMAND_ID, commandId);
		sendCreated(res, bucket.toJson());
	}
}
