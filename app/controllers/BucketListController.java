package controllers;

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
import common.Generator;
import common.Nodes;

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

    public static Result get(String identity) {
        return identity == null ? get() : get(new Identity(identity));
    }

    public static Result get() {
    	Identity identity = IdentityHelper.in(ctx()).get();
    	if (identity == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(identity)) {
    		return forbidden();
    	}
    	ArrayNode array = Nodes.newArray();
    	for (Bucket bucket : buckets.findBuckets()) {
    		ObjectNode object = bucket.toJson();
    		object.put("size", bucket.getSize());
    		array.add(object);
    	}
        return ok(array);
    }

    private static Result get(Identity identity) {
    	Identity current = IdentityHelper.in(ctx()).get();
    	if (current == null) {
    		return unauthorized();
    	}
    	if (!identity.equals(current)) {
    		return forbidden();
    	}
    	ArrayNode array = Nodes.newArray();
    	for (Bucket bucket : buckets.findBuckets(identity)) {
    		ObjectNode object = bucket.toJson();
    		object.put("size", bucket.getSize());
    		array.add(object);
    	}
        return ok(array);
    }

    public static Result post() {
		ObjectNode body = (ObjectNode) request().body().asJson();
		if (body == null || !body.has("label")) {
			return badRequest("missing label");
		}
		Identity identity = IdentityHelper.in(ctx()).get(true);
    	Bucket bucket = createBucket(body.get("label").asText(), identity);
		String commandId = queue.execute(new CreateBucketCommand(buckets, identity, bucket));
        response().setHeader(LOCATION, String.format("/buckets/%s/", bucket.getId()));
        response().setHeader("Undo", String.format("/queue/%s", commandId));
        return created();
    }

	private static Bucket createBucket(String label, Identity identity) {
		String id = Generator.id();
		Bucket bucket = new Bucket(node.getIndex(id), id);
		bucket.setLabel(label);
		bucket.addRole(new Role(identity, "owner"));
		return bucket;
	}
}
