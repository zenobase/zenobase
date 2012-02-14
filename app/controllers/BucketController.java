package controllers;

import java.io.IOException;

import javax.inject.Inject;

import models.Bucket;
import models.Event;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.joda.time.DateTime;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http.StatusCode;
import play.mvc.With;
import queries.BucketQuery;
import queries.BucketResult;
import services.BucketManager;
import services.CommandQueue;
import services.IndexManager;
import services.NodeManager;

import commands.CreateEventCommand;
import commands.DeleteBucketCommand;
import commands.GenerateRandomEventsCommand;
import common.RenderJackson;

@With(UserController.class)
public class BucketController extends Controller {

	@Inject
	static CommandQueue queue;

	@Inject
	static NodeManager node;

	@Inject
	static BucketManager buckets;

	public static void get(String bucketId) {
		Logger.info("Bucket: %s", bucketId);
    	Bucket bucket = buckets.findBucket(bucketId, Security.connected());
    	notFoundIfNull(bucket);
		Logger.info("Bucket: %s", bucket.getId());
    	IndexManager index = node.getIndex(bucketId);
    	BucketResult result = new BucketQuery(bucket).execute(index);
    	renderJson(result.toJson());
    }

	public static void post(String bucketId, ObjectNode body) throws IOException {
		validation.required(bucketId);
    	validation.required(body);
    	if (validation.hasErrors()) {
    		Logger.warn("Rejected: %s", validation.errorsMap());
    		badRequest();
    	}
    	Logger.info("Content: %s", body);
    	Bucket bucket = buckets.findBucket(bucketId, Security.connected());
    	notFoundIfNull(bucket);
    	if (body.has("random")) {
    		String commandId = queue.execute(new GenerateRandomEventsCommand(Security.connected(), bucket, body.get("random").asInt()));
    		response.status = StatusCode.CREATED;
            response.setHeader("Location", String.format("/buckets/%s/", bucket.getId()));
            response.setHeader("Undo", String.format("/queue/%s", commandId));
    	}
    	else {
    		Event event = Event.newEvent(bucket.getId(), body);
    		if (!event.contains(Event.DATE_TIME)) {
    			event.add(Event.DATE_TIME, new DateTime());
    		}
    		String commandId = queue.execute(new CreateEventCommand(bucket, event));
    		response.status = StatusCode.CREATED;
            response.setHeader("Location", String.format("/buckets/%s/%s", bucket.getId(), event.getId()));
            response.setHeader("Undo", String.format("/queue/%s", commandId));
    	}
    }

    public static void delete(String bucketId) {
		Logger.info("Delete: %s", bucketId);
    	Bucket bucket = buckets.findBucket(bucketId, Security.connected());
    	notFoundIfNull(bucket);
    	// TODO: unauthorized() if not owner
    	queue.execute(new DeleteBucketCommand(buckets, bucket));
    	response.status = StatusCode.NO_RESPONSE;
    }

	private static void renderJson(JsonNode object) {
		throw new RenderJackson(object);
	}
}
