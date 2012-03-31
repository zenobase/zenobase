package controllers;

import javax.inject.Inject;

import models.Bucket;

import org.codehaus.jackson.node.ObjectNode;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;
import secure.Identity;
import secure.IdentityHelper;
import secure.UserManager;
import services.BucketManager;
import services.CommandQueue;

import commands.DeleteBucketCommand;

@With(Timed.class)
public class BucketController extends ControllerSupport {

	@Inject
	static CommandQueue queue;

	@Inject
	static BucketManager buckets;

	@Inject
	static UserManager users;

	public static Result get(String bucketId) {
		Identity identity = IdentityHelper.in(ctx()).get();
		return identity != null ? get(bucketId, identity) : unauthorized(); 
    }

	private static Result get(String bucketId, Identity identity) {
		Bucket bucket = buckets.findBucket(bucketId);
    	return bucket != null ? get(bucket, identity) : notFound();
    }

	private static Result get(Bucket bucket, Identity identity) {
    	return bucket.getRole(identity) != null ? ok(bucket.toJson()) : forbidden();
    }

	@BodyParser.Of(value = BodyParser.Json.class, maxLength = 1000)
	public static Result update(String bucketId) {
		
		Identity identity = IdentityHelper.in(ctx()).get();
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
    	if (!"owner".equals(bucket.getRole(identity))) {
    		return forbidden();
    	}
    	buckets.update(bucket);
        return ok(bucket.toJson());
    }

    public static Result delete(String bucketId) {
    	Identity identity = IdentityHelper.in(ctx()).get();
		return identity != null ? delete(bucketId, identity) : unauthorized();
    }

    private static Result delete(String bucketId, Identity identity) {
    	Bucket bucket = buckets.findBucket(bucketId);
    	return bucket != null ? delete(bucket, identity) : notFound();
    }

    private static Result delete(Bucket bucket, Identity identity) {
    	if (!"owner".equals(bucket.getRole(identity)) && !users.isSuperuser(identity)) {
    		return forbidden();
    	}
    	String commandId = queue.execute(new DeleteBucketCommand(buckets, identity, bucket));
        response().setHeader("Undo", String.format("/queue/%s", commandId));
    	return noContent();
    }
}
