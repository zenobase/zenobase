package controllers;

import javax.inject.Inject;

import models.Bucket;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.google.common.base.Objects;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http.StatusCode;
import services.BucketManager;
import services.CommandQueue;
import services.NodeManager;

import commands.CreateBucketCommand;
import common.Generator;
import common.Nodes;
import common.RenderJackson;

public class DashboardController extends Controller {

	@Inject
	static CommandQueue queue;

	@Inject
	static NodeManager node;

	@Inject
	static BucketManager buckets;

    public static void get() {
		String user = Objects.firstNonNull(AuthController.currentUser(), "guest");
    	ArrayNode array = Nodes.newArray();
    	for (Bucket bucket : buckets.findBuckets(user, 0, 10)) {
    		ObjectNode object = bucket.toJson();
    		object.put("size", bucket.getSize());
    		array.add(object);
    	}
        renderJson(array);
    }

    public static void post() {
		String user = AuthController.currentUser();
		if (user == null) {
			forbidden();
		}
		String label = params.get("label");
		validation.required(label);
		validation.minSize(label, 1);
		if (validation.hasErrors()) {
			Logger.warn("Rejected: %s", validation.errorsMap());
			badRequest();
		}
    	Bucket bucket = createBucket(label, user);
		String commandId = queue.execute(new CreateBucketCommand(buckets, bucket, true));
        response.status = StatusCode.CREATED;
        response.setHeader("Location", String.format("/buckets/%s/", bucket.getId()));
        response.setHeader("Undo", String.format("/queue/%s", commandId));
    }

	private static Bucket createBucket(String label, String user) {
		String id = Generator.bucketId();
		Bucket bucket = new Bucket(node.getIndex(id), id);
		bucket.setLabel(label);
		bucket.setUser(user);
		bucket.setRole("owner");
		return bucket;
	}

	private static void renderJson(JsonNode object) {
		throw new RenderJackson(object);
	}
}
