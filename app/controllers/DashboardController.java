package controllers;

import javax.inject.Inject;

import models.Bucket;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http.StatusCode;
import play.mvc.With;
import services.BucketManager;
import services.CommandQueue;
import services.NodeManager;

import commands.CreateBucketCommand;
import common.Generator;
import common.Nodes;
import common.RenderJackson;

@With(UserController.class)
public class DashboardController extends Controller {

	@Inject
	static CommandQueue queue;

	@Inject
	static NodeManager node;

	@Inject
	static BucketManager buckets;

    public static void get() {
    	ArrayNode array = Nodes.newArray();
    	for (Bucket bucket : buckets.findBuckets()) {
    		ObjectNode object = bucket.toJson();
    		object.put("size", bucket.getSize());
    		array.add(object);
    	}
        renderJson(array);
    }

    public static void post() {
		String label = params.get("label");
		validation.required(label);
		validation.minSize(label, 1);
		if (validation.hasErrors()) {
			Logger.warn("Rejected: %s", validation.errorsMap());
			badRequest();
		}
    	Bucket bucket = createBucket(label);
		String commandId = queue.execute(new CreateBucketCommand(buckets, bucket, true));
        response.status = StatusCode.CREATED;
        response.setHeader("Location", String.format("/buckets/%s/", bucket.getId()));
        response.setHeader("Undo", String.format("/queue/%s", commandId));
    }

	private static Bucket createBucket(String label) {
		String id = Generator.bucketId();
		Bucket bucket = new Bucket(node.getIndex(id), id);
		bucket.setLabel(label);
		bucket.setUser(AuthenticationController.connected());
		bucket.setRole("owner");
		return bucket;
	}

	private static void renderJson(JsonNode object) {
		throw new RenderJackson(object);
	}
}
