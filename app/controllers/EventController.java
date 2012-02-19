package controllers;

import javax.inject.Inject;

import models.Bucket;
import models.Event;

import org.codehaus.jackson.node.ObjectNode;

import play.mvc.Result;
import services.BucketManager;

public class EventController extends ControllerSupport {

	@Inject
	static BucketManager manager;

    public static Result get(String bucketId, String eventId) {
		// Logger.info("Event: %s/%s", bucketId, eventId);
    	Bucket bucket = manager.findBucket(bucketId, SecurityController.identity(false));
    	if (bucket == null) {
    		return notFound();
    	}
    	Event event = bucket.findEvent(eventId);
    	if (event == null) {
    		return notFound();
    	}
		ObjectNode eventNode = event.toJson();
		eventNode.put("id", event.getId());
		eventNode.put("bucket", event.getBucket());
    	return ok(eventNode);
    }
}
