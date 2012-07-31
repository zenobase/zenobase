package com.zenobase.controllers;

import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;
import com.google.common.collect.ImmutableList;

import com.zenobase.actions.Timed;
import com.zenobase.commands.DeleteBucketCommand;
import com.zenobase.commands.UpdateBucketCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.models.User;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class BucketController extends ControllerSupport {

	@Inject
	static CommandDispatcher dispatcher;

	@Inject
	static BucketRepository buckets;

	@Inject
	static UserRepository users;

	public static Result get(String bucketId) {
		Identity principal = auth.getPrincipal();
		Bucket bucket = buckets.findBucket(bucketId);
		if (bucket == null) {
			return notFound();
		}
    	if (bucket.getPermission(principal) == Permission.NONE) {
    		return principal == null ? unauthorized() : forbidden();
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
			return ImmutableList.of(timeline(), list(), map());
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

		private ObjectNode map(){
			ObjectNode widget = Nodes.newObject();
			widget.put("id", "default-map");
			widget.put("label", "Map");
			widget.put("type", "map");
			widget.put("placement", "right");
			return widget;
		}
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result update(String bucketId) {
		Identity principal = auth.getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
    	Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound("bucket not found");
    	}
    	if (bucket.getPermission(principal) != Permission.ALL) {
    		return forbidden();
    	}
    	Bucket updated = new Bucket(body());
		if (!updated.valid()) {
			return badRequest("not valid");
		}
		if (!updated.getPrincipals(Permission.ALL).equals(bucket.getPrincipals(Permission.ALL))) {
			return badRequest("not allowed to change the bucket owner");
		}
		if (updated.getPrincipals().size() > 1) {
			User user = users.find(principal);
			if (user == null || !user.isVerified()) {
				return badRequest("not allowed to change permissions");
			}
		}
		String commandId = dispatcher.dispatch(new UpdateBucketCommand(principal, bucket, updated));
		return ok(receipt(commandId));
    }

    public static Result delete(String bucketId) {
    	Identity principal = auth.getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
    	Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getPermission(principal) != Permission.ALL && !users.isSuperuser(principal)) {
    		return forbidden();
    	}
    	String commandId = dispatcher.dispatch(new DeleteBucketCommand(principal, bucket));
    	return ok(receipt(commandId));
    }
}
