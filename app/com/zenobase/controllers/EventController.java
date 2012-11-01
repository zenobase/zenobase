package com.zenobase.controllers;

import javax.inject.Inject;

import org.elasticsearch.index.engine.VersionConflictEngineException;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.commands.DeleteEventCommand;
import com.zenobase.commands.UpdateEventCommand;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;

@With(Timed.class)
public class EventController extends ControllerSupport {

	@Inject
	static BucketRepository buckets;

	@Inject
	static CommandDispatcher dispatcher;

    public static Result get(String bucketId, String eventId) {
    	Identity principal = auth.getPrincipal();
    	if (principal == null) {
    		return unauthorized();
    	}
		Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getPermission(principal) == Permission.NONE) {
    		return forbidden();
    	}
    	Event event = buckets.findEvent(bucketId, eventId);
    	if (event == null) {
    		return notFound();
    	}
    	return ok(event.toJson());
    }

	@BodyParser.Of(BodyParser.Json.class)
	public static Result update(String bucketId, String eventId) {
		Identity principal = auth.getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
    	Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound("bucket not found");
    	}
    	if (bucket.getPermission(principal) != Permission.ALL) {
    		return forbidden();
    	}
    	Event event = buckets.findEvent(bucketId, eventId);
    	if (event == null) {
    		return notFound("event not found");
    	}
    	Event updated = new Event(body());
		updated.setValue(Event.AUTHOR, principal);
		try {
			String commandId = dispatcher.dispatch(new UpdateEventCommand(principal, bucketId, event, updated));
			return success(commandId);
		} catch (VersionConflictEngineException e) {
			return conflict("event is stale");
		}
    }

    public static Result delete(String bucketId, String eventId) {
    	Identity principal = auth.getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
    	Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getPermission(principal) != Permission.ALL) {
    		return forbidden();
    	}
    	Event event = buckets.findEvent(bucket.getId(), eventId);
    	if (event == null) {
    		return notFound();
    	}
    	String commandId = dispatcher.dispatch(new DeleteEventCommand(principal, bucketId, event));
    	return success(commandId);
    }
}
