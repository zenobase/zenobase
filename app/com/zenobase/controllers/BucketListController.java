package com.zenobase.controllers;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.commands.CreateBucketCommand;
import com.zenobase.common.Callback;
import com.zenobase.io.BucketPrinter;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.services.BucketManager;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.IndexManager;
import com.zenobase.services.UserManager;

@With(Timed.class)
public class BucketListController extends ControllerSupport {

	@Inject
	static CommandQueue queue;

	@Inject
	static IndexManager node;

	@Inject
	static BucketManager buckets;

	@Inject
	static UserManager users;

    public static Result find(String identity, int offset, int limit) {
        return identity == null ? find(offset, limit) : find(new Identity(identity), offset, limit);
    }

    private static Result find(int offset, int limit) {
    	Identity principal = auth.getPrincipal();
    	if (principal == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(principal)) {
    		return forbidden();
    	}
    	if (offset == 0 && limit == Integer.MAX_VALUE) {
    		return findAll();
    	}
        return ok(buckets.findBuckets(offset, limit).toJson());
    }

    private static Result find(Identity identity, int offset, int limit) {
    	Identity principal = auth.getPrincipal();
    	if (principal == null) {
    		return unauthorized();
    	}
    	if (!identity.equals(principal) && !users.isSuperuser(identity)) {
    		return forbidden();
    	}
        return ok(buckets.findBuckets(identity, offset, limit).toJson());
    }

    private static Result findAll() {
    	Chunks<String> chunks = new StringChunks() {
			@Override
			public void onReady(final Out<String> out) {
		    	final BucketPrinter printer = new BucketPrinter(out);
				buckets.findBuckets(new Callback<Bucket>() {
					@Override
					public void call(Bucket bucket) {
						printer.print(bucket);
					}
				});
		    	out.close();
			}
		};
        return ok(chunks);
	}

	@BodyParser.Of(BodyParser.Json.class)
    public static Result post() {
		ObjectNode body = body();
		String label = Bucket.LABEL.getValue(body);
		if (label == null) {
			return badRequest("missing field " + Bucket.LABEL);
		}
		String description = Bucket.DESCRIPTION.getValue(body);
		Identity principal = auth.getPrincipal(true);
    	Bucket bucket = createBucket(label, description, principal);
    	String commandId = queue.dispatch(new CreateBucketCommand(principal, bucket));
        response().setHeader(LOCATION, com.zenobase.controllers.routes.BucketController.get(bucket.getId()).toString());
        return created(receipt(commandId));
    }

	private static Bucket createBucket(String label, String description, Identity principal) {
		Bucket bucket = new Bucket();
		bucket.setLabel(label);
		bucket.setDescription(description);
		bucket.addPermission(principal, Permission.ALL);
		return bucket;
	}
}
