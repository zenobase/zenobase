package com.zenobase.controllers;

import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import play.mvc.BodyParser;
import play.mvc.Result;

import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.DeleteAuthorizationCommand;
import com.zenobase.commands.DeleteBucketCommand;
import com.zenobase.commands.DeleteTaskCommand;
import com.zenobase.commands.UpdateBucketCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationQuery;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.TaskQuery;
import com.zenobase.services.TaskRepository;
import com.zenobase.services.UserRepository;

public class BucketController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final BucketRepository buckets;
	private final UserRepository users;
	private final AuthorizationRepository authorizations;
	private final TaskRepository tasks;

	@Inject
	public BucketController(AuthorizationContext security, CommandDispatcher dispatcher,
		BucketRepository buckets, UserRepository users, AuthorizationRepository authorizations,
		TaskRepository tasks) {

		super(security);
		this.dispatcher = dispatcher;
		this.buckets = buckets;
		this.users = users;
		this.authorizations = authorizations;
		this.tasks = tasks;
	}

	public Result get(String bucketId, boolean labelOnly) {
		Authorization auth = getCurrentAuthorization();
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			return notFound();
		}
    	if (!bucket.hasRole(auth, Role.VIEWER)) {
    		return auth == null ? unauthorized() : forbidden();
    	}
		if (bucket.getWidgets().isEmpty()) {
			setDefaultDashboard(bucket);
		}
    	return ok(labelOnly ? toLabel(bucket) : bucket.toJson());
    }

	private static JsonNode toLabel(Bucket bucket) {
		return Nodes.newObject("label", bucket.getLabel());
	}

	public Result get(String bucketId, Function<Bucket, JsonNode> f) {
		Authorization auth = getCurrentAuthorization();
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			return notFound();
		}
    	if (!bucket.hasRole(auth, Role.VIEWER)) {
    		return auth == null ? unauthorized() : forbidden();
    	}
		if (bucket.getWidgets().isEmpty()) {
			setDefaultDashboard(bucket);
		}
    	return ok(f.apply(bucket));
    }

	static void setDefaultDashboard(Bucket bucket) {
		bucket.setWidgets(new DefaultDashboard().widgets());
	}

	private static class DefaultDashboard {

		public List<ObjectNode> widgets(){
			return ImmutableList.of(timeline(), list(), count());
		}

		private ObjectNode list(){
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

		private ObjectNode timeline(){
			ObjectNode widget = Nodes.newObject();
			widget.put("id", "timeline");
			widget.put("label", "Timeline");
			widget.put("type", "timeline");
			widget.put("placement", "top");
			widget.put("field", "timestamp");
			widget.put("statistic", "count");
			return widget;
		}

		private ObjectNode count(){
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

	@BodyParser.Of(BodyParser.Json.class)
	public Result update(String bucketId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
    	Bucket bucket = buckets.find(bucketId);
    	if (bucket == null) {
    		return notFound("bucket not found");
    	}
    	if (!bucket.hasRole(auth, Role.OWNER)) {
    		return forbidden();
    	}
    	Bucket updated = new Bucket(body());
		if (!updated.valid()) {
			return badRequest("bucket not valid");
		}
		if (!updated.getPrincipals(Role.OWNER).equals(bucket.getPrincipals(Role.OWNER))) {
			return badRequest("bucket owner can't change");
		}
		if (updated.getPrincipals().size() > 1) {
			User user = users.find(auth.getPrincipal());
			if (user == null || !user.isVerified()) {
				return forbidden("not permitted to add or remove roles");
			}
		}
		if (bucket.getAliases().isEmpty() != updated.getAliases().isEmpty()) {
			return badRequest("can't change bucket type");
		}
		if (!new AliasValidator(buckets).checkPermissions(updated, auth)) {
			return badRequest("one or more aliases are invalid");
		}
		try {
			String commandId = dispatcher.dispatch(new UpdateBucketCommand(auth.getPrincipal(), bucket, updated));
    		response().setHeader(COMMAND_ID, commandId);
			return noContent();
		} catch (VersionConflictEngineException e) {
			return conflict("bucket is stale");
		}
    }

	public Result delete(String bucketId) {
    	Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
    	Bucket bucket = buckets.find(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (!bucket.hasRole(auth, Role.OWNER) && !users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
    	if (buckets.isAliased(bucketId)) {
    		return conflict("bucket is aliased");
    	}
    	CompoundCommand command = new CompoundCommand(auth.getPrincipal(), "deleted bucket and associated data", "restored bucket and associated data");
    	authorizations.find(new AuthorizationQuery().scopeEqualTo(bucket.getId()), authorization -> command.add(new DeleteAuthorizationCommand(auth.getPrincipal(), authorization)));
    	tasks.find(new TaskQuery().bucketEqualTo(bucketId), task -> command.add(new DeleteTaskCommand(auth.getPrincipal(), task)));
    	command.add(new DeleteBucketCommand(auth.getPrincipal(), bucket));
    	String commandId = dispatcher.dispatch(command.unwrap());
		response().setHeader(COMMAND_ID, commandId);
    	return noContent();
    }
}
