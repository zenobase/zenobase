package controllers;

import javax.inject.Inject;

import models.Bucket;
import models.Permission;

import org.codehaus.jackson.node.ObjectNode;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;
import models.Identity;
import services.BucketManager;
import services.CommandQueue;
import services.UserManager;

import commands.DeleteBucketCommand;
import commands.UpdateBucketCommand;
import common.DefaultDashboard;
import common.Identities;

@With(Timed.class)
public class BucketController extends ControllerSupport {

	@Inject
	static CommandQueue queue;

	@Inject
	static BucketManager buckets;

	@Inject
	static UserManager users;

	public static Result get(String bucketId) {
		Identity identity = Identities.in(ctx()).get();
		return identity != null ? get(bucketId, identity) : unauthorized(); 
    }

	private static Result get(String bucketId, Identity identity) {
		Bucket bucket = buckets.findBucket(bucketId);
    	return bucket != null ? get(bucket, identity) : notFound();
    }

	private static Result get(Bucket bucket, Identity identity) {
    	if (bucket.getPermission(identity) == Permission.NONE) {
    		return forbidden();
    	}
		if (bucket.getWidgets().isEmpty()) {
			bucket.setWidgets(new DefaultDashboard().widgets());
		}
    	return ok(bucket.toJson());
    }

	@BodyParser.Of(value = BodyParser.Json.class, maxLength = 10000)
	public static Result update(String bucketId) {
		
		Identity identity = Identities.in(ctx()).get();
		if (identity == null) {
			return unauthorized();
		}
		ObjectNode body = (ObjectNode) request().body().asJson();
		if (body == null) {
			return badRequest();
		}
    	Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getPermission(identity) != Permission.ALL) {
    		return forbidden();
    	}
		String commandId = queue.execute(new UpdateBucketCommand(buckets, identity, bucket, new Bucket(body)));
        response().setHeader("Undo", String.format("/queue/%s", commandId));
		return noContent();
    }

    public static Result delete(String bucketId) {
    	Identity identity = Identities.in(ctx()).get();
		return identity != null ? delete(bucketId, identity) : unauthorized();
    }

    private static Result delete(String bucketId, Identity identity) {
    	Bucket bucket = buckets.findBucket(bucketId);
    	return bucket != null ? delete(bucket, identity) : notFound();
    }

    private static Result delete(Bucket bucket, Identity identity) {
    	if (bucket.getPermission(identity) != Permission.ALL && !users.isSuperuser(identity)) {
    		return forbidden();
    	}
    	String commandId = queue.execute(new DeleteBucketCommand(buckets, identity, bucket));
        response().setHeader("Undo", String.format("/queue/%s", commandId));
    	return noContent();
    }
}
