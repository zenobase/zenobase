package com.zenobase.controllers;

import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;
import com.google.common.collect.ImmutableList;

import com.zenobase.actions.Timed;
import com.zenobase.commands.DeleteBucketCommand;
import com.zenobase.commands.UpdateBucketCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Permission;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class BucketController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final BucketRepository buckets;
	private final UserRepository users;

	@Inject
	public BucketController(AuthorizationContext security, CommandDispatcher dispatcher,
		BucketRepository buckets, UserRepository users) {

		super(security);
		this.dispatcher = dispatcher;
		this.buckets = buckets;
		this.users = users;
	}

	public Result get(String bucketId) {
		Authorization auth = getCurrentAuthorization();
		Bucket bucket = buckets.findBucket(bucketId);
		if (bucket == null) {
			return notFound();
		}
    	if (!bucket.isPermitted(auth, Permission.USE)) {
    		return auth == null ? unauthorized() : forbidden();
    	}
		if (bucket.getWidgets().isEmpty()) {
			setDefaultDashboard(bucket);
		}
    	return ok(bucket.toJson());
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
			widget.put("id", "default-list");
			widget.put("label", "Latest");
			widget.put("type", "list");
			widget.put("placement", "left");
			widget.put("singleton", true);
			widget.put("limit", 10);
			widget.put("order", "timestamp");
			widget.put("reverse", false);
			return widget;
		}

		private ObjectNode timeline(){
			ObjectNode widget = Nodes.newObject();
			widget.put("id", "default-timeline");
			widget.put("label", "Timeline");
			widget.put("type", "timeline");
			widget.put("placement", "top");
			widget.put("valueField", "timestamp");
			widget.put("statistic", "count");
			return widget;
		}

		private ObjectNode count(){
			ObjectNode widget = Nodes.newObject();
			widget.put("id", "default-count");
			widget.put("label", "Tags");
			widget.put("type", "count");
			widget.put("field", "tag");
			widget.put("order", "count");
			widget.put("reverse", false);
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
    	Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound("bucket not found");
    	}
    	if (!bucket.isPermitted(auth, Permission.ALL)) {
    		return forbidden();
    	}
    	Bucket updated = new Bucket(body());
		if (!updated.valid()) {
			return badRequest("bucket not valid");
		}
		if (!updated.getPrincipals(Permission.ALL).equals(bucket.getPrincipals(Permission.ALL))) {
			return badRequest("bucket owner can't change");
		}
		if (updated.getPrincipals().size() > 1) {
			User user = users.find(auth.getPrincipal());
			if (user == null || !user.isVerified()) {
				return forbidden("not permitted to change permissions on this bucket");
			}
		}
		try {
			String commandId = dispatcher.dispatch(new UpdateBucketCommand(auth.getPrincipal(), bucket, updated));
			return success(commandId);
		} catch (VersionConflictEngineException e) {
			return conflict("bucket is stale");
		}
    }

    public Result delete(String bucketId) {
    	Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
    	Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (!bucket.isPermitted(auth, Permission.ALL) && !users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
    	String commandId = dispatcher.dispatch(new DeleteBucketCommand(auth.getPrincipal(), bucket));
    	return success(commandId);
    }
}
