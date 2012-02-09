package controllers;

import java.io.IOException;

import javax.inject.Inject;

import models.Bucket;
import models.Event;

import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import queries.BucketQuery;
import queries.BucketResult;
import services.BucketManager;
import services.CommandQueue;
import services.NodeManager;

import commands.CreateEventCommand;
import commands.GenerateRandomEventsCommand;

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
    	BucketResult result = new BucketQuery(bucketId).execute(node.getIndex(bucketId));
    	renderTemplate("bucket.html", bucket, result);
    }

    public static void post(String bucketId, ObjectNode content) throws IOException {
    	validation.required(bucketId);
    	validation.required(content);
    	Logger.info("Content: %s", content);
    	Bucket bucket = buckets.findBucket(bucketId, Security.connected());
    	notFoundIfNull(bucket);
    	if (validation.hasErrors()) {
    		Logger.warn("Failed: %s", validation.errors());
    		params.flash();
    		validation.keep();
    		get(bucketId);
    	}
    	if (content.has("random")) {
        	queue.execute(new GenerateRandomEventsCommand(Security.connected(), bucket, content.get("random").asInt()));
        	get(bucketId);
    	}
    	else {
    		Event event = Event.newEvent(bucket.getId(), content);
    		queue.execute(new CreateEventCommand(bucket, event));
    		EventController.get(bucketId, event.getId());
    	}
    }

    public static void delete(String bucketId) {
		Logger.info("Delete: %s", bucketId);
    	buckets.deleteBucket(bucketId, Security.connected());
    	flash.put("confirmation", "Deleted bucket '" + bucketId + "'");
    	DashboardController.get();
    }
}
