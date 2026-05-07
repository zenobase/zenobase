package com.zenobase.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.DeleteBucketCommand;
import com.zenobase.commands.DeleteTaskCommand;
import com.zenobase.commands.UpdateBucketCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.TaskQuery;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.TaskRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import org.opensearch.client.opensearch._types.OpenSearchException;

public class BucketController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final BucketRepository buckets;
	private final UserRepository users;
	private final TaskRepository tasks;

	@Inject
	public BucketController(
		AuthorizationContext security,
		CommandDispatcher dispatcher,
		BucketRepository buckets,
		UserRepository users,
		TaskRepository tasks
	) {
		super(security);
		this.dispatcher = dispatcher;
		this.buckets = buckets;
		this.users = users;
		this.tasks = tasks;
	}

	public void get(ServerRequest req, ServerResponse res) {
		String bucketId = req.path().pathParameters().get("bucketId");
		handleGet(req, res, bucketId, false);
	}

	public void getLabel(ServerRequest req, ServerResponse res) {
		String bucketId = req.path().pathParameters().get("bucketId");
		handleGet(req, res, bucketId, true);
	}

	private void handleGet(ServerRequest req, ServerResponse res, String bucketId, boolean labelOnly) {
		Authorization auth = getCurrentAuthorization(req);
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			sendNotFound(res);
			return;
		}
		if (!bucket.hasRole(auth, Role.VIEWER)) {
			if (auth == null) {
				sendUnauthorized(res);
			} else {
				sendForbidden(res);
			}
			return;
		}
		if (bucket.getWidgets().isEmpty()) {
			setDefaultDashboard(bucket);
		}
		sendOk(res, labelOnly ? toLabel(bucket) : bucket.toJson());
	}

	private static JsonNode toLabel(Bucket bucket) {
		return Nodes.newObject("label", Objects.requireNonNull(bucket.getLabel()));
	}

	static void setDefaultDashboard(Bucket bucket) {
		bucket.setWidgets(new DefaultDashboard().widgets());
	}

	private static class DefaultDashboard {

		public List<ObjectNode> widgets() {
			return List.of(timeline(), list(), count());
		}

		private ObjectNode list() {
			ObjectNode widget = Nodes.newObject();
			widget.put("id", "events");
			widget.put("label", "Latest");
			widget.put("type", "list");
			widget.put("placement", "left");
			widget.put("singleton", true);
			widget.put("limit", 10);
			widget.put("order", "-timestamp");
			return widget;
		}

		private ObjectNode timeline() {
			ObjectNode widget = Nodes.newObject();
			widget.put("id", "timeline");
			widget.put("label", "Timeline");
			widget.put("type", "timeline");
			widget.put("placement", "top");
			widget.put("field", "timestamp");
			widget.put("statistic", "count");
			return widget;
		}

		private ObjectNode count() {
			ObjectNode widget = Nodes.newObject();
			widget.put("id", "tags");
			widget.put("label", "Tags");
			widget.put("type", "count");
			widget.put("field", "tag");
			widget.put("order", "-count");
			widget.put("limit", 10);
			widget.put("placement", "right");
			return widget;
		}
	}

	public void update(ServerRequest req, ServerResponse res) {
		String bucketId = req.path().pathParameters().get("bucketId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			sendNotFound(res, "bucket not found");
			return;
		}
		if (!bucket.hasRole(auth, Role.OWNER)) {
			sendForbidden(res);
			return;
		}
		Bucket updated = new Bucket(body(req));
		if (!updated.valid()) {
			sendBadRequest(res, "bucket not valid");
			return;
		}
		if (bucket.isArchived() && updated.isArchived()) {
			sendConflict(res, "bucket is archived");
			return;
		}
		if (!updated.getPrincipals(Role.OWNER).equals(bucket.getPrincipals(Role.OWNER))) {
			sendBadRequest(res, "bucket owner can't change");
			return;
		}
		if (updated.getPrincipals().size() > 1) {
			User user = users.find(auth.getPrincipal());
			if (user == null || !user.isVerified()) {
				sendForbidden(res, "not permitted to add or remove roles");
				return;
			}
		}
		if (bucket.getAliases().isEmpty() != updated.getAliases().isEmpty()) {
			sendBadRequest(res, "can't change bucket type");
			return;
		}
		if (!new AliasValidator(buckets).checkPermissions(updated, auth)) {
			sendBadRequest(res, "one or more aliases are invalid");
			return;
		}
		if (sendForbiddenIfSuspended(auth, res)) {
			return;
		}
		try {
			String commandId = dispatcher.dispatch(new UpdateBucketCommand(auth.getPrincipal(), bucket, updated));
			setHeader(res, COMMAND_ID, commandId);
			sendNoContent(res);
		} catch (OpenSearchException e) {
			if (e.status() == 409) {
				sendConflict(res, "bucket is stale");
				return;
			}
			throw e;
		}
	}

	public void delete(ServerRequest req, ServerResponse res) {
		String bucketId = req.path().pathParameters().get("bucketId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			sendNotFound(res);
			return;
		}
		if (!bucket.hasRole(auth, Role.OWNER) && !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		if (buckets.isAliased(bucketId)) {
			sendConflict(res, "bucket is aliased");
			return;
		}
		if (sendForbiddenIfSuspended(auth, res)) {
			return;
		}
		CompoundCommand command = new CompoundCommand(
			auth.getPrincipal(),
			"deleted bucket and associated data",
			"restored bucket and associated data"
		);
		tasks.find(new TaskQuery().bucketEqualTo(bucketId), task ->
			command.add(new DeleteTaskCommand(auth.getPrincipal(), task))
		);
		command.add(new DeleteBucketCommand(auth.getPrincipal(), bucket));
		String commandId = dispatcher.dispatch(command.unwrap());
		setHeader(res, COMMAND_ID, commandId);
		sendNoContent(res);
	}
}
