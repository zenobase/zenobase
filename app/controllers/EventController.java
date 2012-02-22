package controllers;

import javax.inject.Inject;

import models.Bucket;
import models.Event;
import play.mvc.Result;
import play.mvc.With;
import services.BucketManager;

@With(Timed.class)
public class EventController extends ControllerSupport {

	@Inject
	static BucketManager manager;

    public static Result get(String bucketId, String eventId) {
    	Bucket bucket = manager.findBucket(bucketId, SecurityController.identity(false));
    	if (bucket == null) {
    		return notFound();
    	}
    	Event event = bucket.findEvent(eventId);
    	if (event == null) {
    		return notFound();
    	}
    	return ok(event.toJson());
    }
}
