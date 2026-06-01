package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.Ints;
import com.zenobase.commands.CreateBucketCommand;
import com.zenobase.common.PartialList;
import com.zenobase.json.LongField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.BucketList;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.BucketQuery;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.EventRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.SearchOrder;
import com.zenobase.services.UserLookup;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BucketListController extends ControllerSupport {

	private static final LongField SIZE = new LongField("size");

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
		UserRepository users
	) {
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
		BucketQuery query = new BucketQuery();
		if (q != null) {
			query = query.queryString(q);
		}
		sendOk(res, toJson(buckets.find(query, BucketQuery.DEFAULT_ORDER, offset, limit)));
	}

	public void findByUser(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		String order = req.query().first("order").orElse(null);
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));
		boolean labelsOnly = Boolean.parseBoolean(req.query().first("labels_only").orElse("false"));
		boolean includeArchived = Boolean.parseBoolean(req.query().first("include_archived").orElse("false"));
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
		sendOk(res, labelsOnly ? BucketList.toJson(found) : toJson(found));
	}

	private ObjectNode toJson(PartialList<Bucket> list) {
		ObjectNode resultNode = Nodes.newObject();
		PartialList.TOTAL.setValue(resultNode, Ints.checkedCast(list.getTotal()));
		ArrayNode bucketsNode = resultNode.putArray("buckets");
		List<String> bucketIds = list.stream().map(Bucket::getId).collect(Collectors.toList());
		Map<String, Long> sizes = events.sizes(bucketIds);
		for (Bucket bucket : list) {
			ObjectNode bucketNode = bucket.toJson();
			SIZE.setValue(bucketNode, sizes.getOrDefault(bucket.getId(), 0L));
			bucketsNode.add(bucketNode);
		}
		return resultNode;
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
		if (sendForbiddenIfSuspended(auth, res)) {
			return;
		}
		String commandId = dispatcher.dispatch(new CreateBucketCommand(auth.getPrincipal(), bucket));
		setHeader(res, LOCATION, "/buckets/" + bucket.getId());
		setHeader(res, COMMAND_ID, commandId);
		sendCreated(res, bucket.toJson());
	}
}
