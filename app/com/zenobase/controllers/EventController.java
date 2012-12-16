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
import com.zenobase.models.Permission;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;

@With(Timed.class)
public class EventController extends ControllerSupport {

	private final BucketRepository buckets;
	private final CommandDispatcher dispatcher;

	@Inject
    public EventController(AuthorizationContext security, BucketRepository buckets, CommandDispatcher dispatcher) {
		super(security);
		this.buckets = buckets;
		this.dispatcher = dispatcher;
	}

	public Result get(String bucketId, String eventId) {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
		Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (!bucket.isPermitted(auth, Permission.USE)) {
    		return forbidden();
    	}
    	Event event = buckets.findEvent(bucketId, eventId);
    	if (event == null) {
    		return notFound();
    	}
    	return ok(event.toJson());
    }

	@BodyParser.Of(BodyParser.Json.class)
	public Result update(String bucketId, String eventId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
    	Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound("bucket not found");
    	}
    	if (!bucket.isPermitted(auth, Permission.ALL)) {
    		return forbidden();
    	}
    	Event event = buckets.findEvent(bucketId, eventId);
    	if (event == null) {
    		return notFound("event not found");
    	}
    	Event updated = new Event(body());
		updated.setValue(Event.AUTHOR, auth.getPrincipal());
		try {
			String commandId = dispatcher.dispatch(new UpdateEventCommand(auth.getPrincipal(), bucketId, event, updated));
    		response().setHeader(COMMAND_ID, commandId);
			return noContent();
		} catch (VersionConflictEngineException e) {
			return conflict("event is stale");
		}
    }

    public Result delete(String bucketId, String eventId) {
    	Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
    	Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (!bucket.isPermitted(auth, Permission.ALL)) {
    		return forbidden();
    	}
    	Event event = buckets.findEvent(bucket.getId(), eventId);
    	if (event == null) {
    		return notFound();
    	}
    	String commandId = dispatcher.dispatch(new DeleteEventCommand(auth.getPrincipal(), bucketId, event));
		response().setHeader(COMMAND_ID, commandId);
    	return noContent();
    }
}
