package controllers;

import javax.inject.Inject;

import models.Bucket;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import play.mvc.Result;
import play.mvc.With;
import secure.Identity;
import services.BucketManager;
import services.CommandQueue;
import services.NodeManager;

import commands.CreateBucketCommand;
import common.Generator;
import common.Nodes;

@With(Timed.class)
public class DashboardController extends ControllerSupport {

	@Inject
	static CommandQueue queue;

	@Inject
	static NodeManager node;

	@Inject
	static BucketManager buckets;

    public static Result get() {
    	ArrayNode array = Nodes.newArray();
    	Identity identity = SecurityController.identity(false);
    	if (identity != null) {
	    	for (Bucket bucket : buckets.findBuckets(identity)) {
	    		ObjectNode object = bucket.toJson();
	    		object.put("size", bucket.getSize());
	    		array.add(object);
	    	}
    	}
        return ok(array);
    }

    public static Result post() {
		ObjectNode body = (ObjectNode) request().body().asJson();
		if (body == null || !body.has("label")) {
			return badRequest("missing label");
		}
		Identity identity = SecurityController.identity(true);
    	Bucket bucket = createBucket(body.get("label").asText(), identity);
		String commandId = queue.execute(new CreateBucketCommand(buckets, bucket, true));
        response().setHeader(LOCATION, String.format("/buckets/%s/", bucket.getId()));
        response().setHeader("Undo", String.format("/queue/%s", commandId));
        return created();
    }

	private static Bucket createBucket(String label, Identity identity) {
		String id = Generator.bucketId();
		Bucket bucket = new Bucket(node.getIndex(id), id);
		bucket.setLabel(label);
		bucket.setIdentity(identity);
		bucket.setRole("owner");
		return bucket;
	}
}
