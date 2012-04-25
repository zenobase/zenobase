package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.commands.DeleteBucketCommand;
import com.zenobase.commands.UpdateBucketCommand;
import com.zenobase.common.DefaultDashboard;
import com.zenobase.common.SecurityContext;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.services.BucketManager;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.UserManager;

@With(Timed.class)
public class BucketController extends ControllerSupport {

	@Inject
	static CommandQueue queue;

	@Inject
	static BucketManager buckets;

	@Inject
	static UserManager users;

	public static Result get(String bucketId) {
		Identity principal = new SecurityContext(ctx()).getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
		Bucket bucket = buckets.findBucket(bucketId);
		if (bucket == null) {
			return notFound();
		}
    	if (bucket.getPermission(principal) == Permission.NONE) {
    		return forbidden();
    	}
		if (bucket.getWidgets().isEmpty()) {
			bucket.setWidgets(new DefaultDashboard().widgets());
		}
    	return ok(bucket.toJson());
    }

	@BodyParser.Of(BodyParser.Json.class)
	public static Result update(String bucketId) {
		Identity principal = new SecurityContext(ctx()).getPrincipal();
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
		String commandId = queue.dispatch(new UpdateBucketCommand(principal, bucket, new Bucket(body())));
		return ok(receipt(commandId));
    }

    public static Result delete(String bucketId) {
    	Identity principal = new SecurityContext(ctx()).getPrincipal();
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
    	String commandId = queue.dispatch(new DeleteBucketCommand(principal, bucket));
    	return ok(receipt(commandId));
    }
}
