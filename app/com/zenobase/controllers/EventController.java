package com.zenobase.controllers;

import javax.inject.Inject;

import org.opensearch.client.opensearch._types.OpenSearchException;
import play.mvc.BodyParser;
import play.mvc.Result;

import com.zenobase.commands.DeleteEventCommand;
import com.zenobase.commands.UpdateEventCommand;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.EventRepository;

public class EventController extends ControllerSupport {

	private final BucketRepository buckets;
	private final EventRepository events;
	private final CommandDispatcher dispatcher;

	@Inject
    public EventController(AuthorizationContext security, BucketRepository buckets, EventRepository events, CommandDispatcher dispatcher) {
		super(security);
		this.buckets = buckets;
		this.events = events;
		this.dispatcher = dispatcher;
	}

	public Result get(String bucketId, String eventId) {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
		Bucket bucket = buckets.find(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (!bucket.hasRole(auth, Role.VIEWER)) {
    		return forbidden();
    	}
    	Event event = events.find(bucketId, eventId);
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
    	Bucket bucket = buckets.find(bucketId);
    	if (bucket == null) {
    		return notFound("bucket not found");
    	}
    	if (!bucket.hasRole(auth, Role.OWNER)) {
    		return forbidden();
    	}
    	Event event = events.find(bucketId, eventId);
    	if (event == null) {
    		return notFound("event not found");
    	}
    	Event updated = new Event(body());
		updated.setValue(Event.AUTHOR, auth.getPrincipal());
		try {
			String commandId = dispatcher.dispatch(new UpdateEventCommand(auth.getPrincipal(), bucketId, event, updated));
    		response().setHeader(COMMAND_ID, commandId);
			return noContent();
		} catch (OpenSearchException e) {
			if (e.status() == 409) return conflict("event is stale");
			throw e;
		}
    }

    public Result delete(String bucketId, String eventId) {
    	Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
    	Bucket bucket = buckets.find(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (!bucket.hasRole(auth, Role.OWNER)) {
    		return forbidden();
    	}
    	Event event = events.find(bucket.getId(), eventId);
    	if (event == null) {
    		return notFound();
    	}
    	String commandId = dispatcher.dispatch(new DeleteEventCommand(auth.getPrincipal(), bucketId, event));
		response().setHeader(COMMAND_ID, commandId);
    	return noContent();
    }
}
