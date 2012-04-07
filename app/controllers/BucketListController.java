package controllers;


import io.BucketPrinter;

import javax.inject.Inject;

import models.Bucket;
import models.Permission;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.mvc.Result;
import play.mvc.With;
import models.Identity;
import services.BucketManager;
import services.CommandQueue;
import services.IndexManager;
import services.UserManager;

import commands.CreateBucketCommand;
import common.Callback;
import common.Generator;
import common.Identities;
import common.Nodes;
import common.PartialList;

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
    	Identity identity = Identities.in(ctx()).get();
    	if (identity == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(identity)) {
    		return forbidden();
    	}
    	if (offset == 0 && limit == Integer.MAX_VALUE) {
    		return findAll();
    	}
        return ok(toJson(buckets.findBuckets(offset, limit)));
    }

    private static Result find(Identity identity, int offset, int limit) {
    	Identity current = Identities.in(ctx()).get();
    	if (current == null) {
    		return unauthorized();
    	}
    	if (!identity.equals(current) && !users.isSuperuser(identity)) {
    		return forbidden();
    	}
        return ok(toJson(buckets.findBuckets(identity, offset, limit)));
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

    private static ObjectNode toJson(PartialList<Bucket> results) {
    	ObjectNode resultNode = Nodes.newObject();
    	resultNode.put("total", results.size());
    	ArrayNode bucketsNode = resultNode.putArray("buckets");
    	for (Bucket bucket : results.getElements()) {
    		ObjectNode bucketNode = bucket.toJson();
    		bucketNode.put("size", buckets.getSize(bucket.getId()));
    		bucketsNode.add(bucketNode);
    	}
    	return resultNode;
    }

    public static Result post() {
		ObjectNode body = (ObjectNode) request().body().asJson();
		if (body == null) {
			return badRequest("missing request body");
		}
		Bucket update = new Bucket(body);
		String label = update.getLabel();
		String description = update.getDescription();
		if (label == null) {
			return badRequest("missing label");
		}
		Identity identity = Identities.in(ctx()).get(true);
    	Bucket bucket = createBucket(label, description, identity);
    	String commandId = queue.execute(new CreateBucketCommand(buckets, identity, bucket));
        response().setHeader(LOCATION, String.format("/buckets/%s/", bucket.getId()));
        response().setHeader("Undo", String.format("/queue/%s", commandId));
        return created();
    }

	private static Bucket createBucket(String label, String description, Identity identity) {
		Bucket bucket = new Bucket(Generator.id());
		bucket.setLabel(label);
		bucket.setDescription(description);
		bucket.addPermission(identity, Permission.ALL);
		Logger.info("Bucket: " + bucket.getPermission(identity));
		return bucket;
	}
}
