package controllers;

import java.io.IOException;

import javax.inject.Inject;

import models.Bucket;
import models.Event;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.mvc.Controller;
import services.BucketManager;

import common.RenderJackson;

public class EventController extends Controller {

	@Inject
	static BucketManager manager;

    public static void get(String bucketId, String eventId) throws IOException {
		Logger.info("Event: %s/%s", bucketId, eventId);
    	Bucket bucket = manager.findBucket(bucketId, AuthController.currentUser());
    	notFoundIfNull(bucket);
    	Event event = bucket.findEvent(eventId);
    	notFoundIfNull(event);    	
		ObjectNode eventNode = event.toJson();
		eventNode.put("id", event.getId());
		eventNode.put("bucket", event.getBucket());
    	renderJson(eventNode);
    }

	private static void renderJson(JsonNode object) {
		throw new RenderJackson(object);
	}
}
