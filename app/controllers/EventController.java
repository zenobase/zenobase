package controllers;

import javax.inject.Inject;

import models.Bucket;
import models.Event;
import play.mvc.Result;
import play.mvc.With;
import secure.Identity;
import secure.IdentityHelper;
import services.BucketManager;
import services.CommandQueue;

import commands.DeleteEventCommand;

@With(Timed.class)
public class EventController extends ControllerSupport {

	@Inject
	static BucketManager manager;

	@Inject
	static CommandQueue queue;

    public static Result get(String bucketId, String eventId) {
		Bucket bucket = manager.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getRole(IdentityHelper.in(ctx()).get()) == null) {
    		return forbidden();
    	}
    	Event event = bucket.findEvent(eventId);
    	if (event == null) {
    		return notFound();
    	}
    	return ok(event.toJson());
    }

    public static Result delete(String bucketId, String eventId) {
    	Identity identity = IdentityHelper.in(ctx()).get();
		return identity != null ? delete(bucketId, eventId, identity) : unauthorized();
    }

    private static Result delete(String bucketId, String eventId, Identity identity) {
    	Bucket bucket = manager.findBucket(bucketId);
    	return bucket != null ? delete(bucket, eventId, identity) : notFound();
    }

    private static Result delete(Bucket bucket, String eventId, Identity identity) {
    	if (!"owner".equals(bucket.getRole(identity))) {
    		return forbidden();
    	}
    	Event event = bucket.findEvent(eventId);
    	return event != null ? delete(bucket, event, identity) : notFound();
    }

    private static Result delete(Bucket bucket, Event event, Identity identity) {
    	queue.execute(new DeleteEventCommand(bucket, identity, event));
    	return noContent();
    }
}
