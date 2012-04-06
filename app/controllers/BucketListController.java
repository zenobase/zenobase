package controllers;


import io.BucketPrinter;

import javax.inject.Inject;

import models.Bucket;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import play.mvc.Result;
import play.mvc.With;
import secure.Identity;
import secure.IdentityHelper;
import secure.Role;
import secure.UserManager;
import services.BucketManager;
import services.CommandQueue;
import services.NodeManager;

import commands.CreateBucketCommand;
import common.Callback;
import common.Generator;
import common.Nodes;
import common.PartialList;

@With(Timed.class)
public class BucketListController extends ControllerSupport {

	@Inject
	static CommandQueue queue;

	@Inject
	static NodeManager node;

	@Inject
	static BucketManager buckets;

	@Inject
	static UserManager users;

    public static Result find(String identity, int offset, int limit) {
        return identity == null ? find(offset, limit) : find(new Identity(identity), offset, limit);
    }

    private static Result find(int offset, int limit) {
    	Identity identity = IdentityHelper.in(ctx()).get();
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
    	Identity current = IdentityHelper.in(ctx()).get();
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
		if (body == null || !body.has("label")) {
			return badRequest("missing request body");
		}
		String label = body.findPath(Bucket.LABEL.getName()).getTextValue();
		String description = body.findPath(Bucket.DESCRIPTION.getName()).getTextValue();
		if (label == null) { // TODO validate label
			return badRequest("missing label");
		}
		Identity identity = IdentityHelper.in(ctx()).get(true);
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
		bucket.addRole(new Role(identity, Role.OWNER));
		return bucket;
	}
}
