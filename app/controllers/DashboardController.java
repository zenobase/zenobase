package controllers;

import javax.inject.Inject;

import models.Bucket;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import play.mvc.Result;
import services.BucketManager;
import services.CommandQueue;
import services.NodeManager;

import com.google.common.base.Objects;
import commands.CreateBucketCommand;
import common.Generator;
import common.Nodes;

public class DashboardController extends ControllerSupport {

	@Inject
	static CommandQueue queue;

	@Inject
	static NodeManager node;

	@Inject
	static BucketManager buckets;

    public static Result get() {
		String user = Objects.firstNonNull(SecurityController.user(), "guest");
    	ArrayNode array = Nodes.newArray();
    	for (Bucket bucket : buckets.findBuckets(user)) {
    		ObjectNode object = bucket.toJson();
    		object.put("size", bucket.getSize());
    		array.add(object);
    	}
        return ok(array);
    }

    public static Result post() {
    	String user = SecurityController.user();
		if (user == null) {
			forbidden();
		}
		ObjectNode body = (ObjectNode) request().body().asJson();
		if (body == null || !body.has("label")) {
			return badRequest("missing label");
		}
    	Bucket bucket = createBucket(body.get("label").asText(), user);
		String commandId = queue.execute(new CreateBucketCommand(buckets, bucket, true));
        response().setHeader(LOCATION, String.format("/buckets/%s/", bucket.getId()));
        response().setHeader("Undo", String.format("/queue/%s", commandId));
        return created();
    }

	private static Bucket createBucket(String label, String user) {
		String id = Generator.bucketId();
		Bucket bucket = new Bucket(node.getIndex(id), id);
		bucket.setLabel(label);
		bucket.setUser(user);
		bucket.setRole("owner");
		return bucket;
	}
}
