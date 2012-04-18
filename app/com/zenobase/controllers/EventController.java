package com.zenobase.controllers;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.DeleteEventCommand;
import com.zenobase.commands.RandomEventsCommandBuilder;
import com.zenobase.common.Generator;
import com.zenobase.common.SecurityContext;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.search.EventSearch;
import com.zenobase.services.BucketManager;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.IndexManager;

@With(Timed.class)
public class EventController extends ControllerSupport {

	@Inject
	static BucketManager manager;

	@Inject
	static IndexManager node;

	@Inject
	static CommandQueue queue;

	public static Result find(String bucketId) {
		Identity principal = new SecurityContext(ctx()).getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
		Bucket bucket = manager.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	return bucket.getPermission(principal) != Permission.NONE ? ok(new EventSearch()
			.addWidgets(request().queryString().get("w"))
			.addFilters(request().queryString().get("q"))
			.execute(node.getIndex(bucket.getId()))) : forbidden();
    }

	@BodyParser.Of(value = BodyParser.Json.class, maxLength = 1000)
	public static Result post(String bucketId) {
		Identity principal = new SecurityContext(ctx()).getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
		ObjectNode body = (ObjectNode) request().body().asJson();
		if (body == null) {
			return badRequest();
		}
    	Bucket bucket = manager.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getPermission(principal) != Permission.ALL) {
    		return forbidden();
    	}
    	if (body.has("random")) {
    		String commandId = queue.dispatch(new RandomEventsCommandBuilder(principal, bucketId).build(body.get("random").asInt()));
            response().setHeader(LOCATION, String.format("/buckets/%s/", bucket.getId()));
            response().setHeader("Undo", String.format("/queue/%s", commandId));
            return created();
    	}
    	else {
    		Event event = new Event(body);
			event.setValue(Event.ID, Generator.id());
			event.setValue(Event.AUTHOR, principal);
    		if (!event.contains(Event.TIMESTAMP)) {
    			event.addValue(Event.TIMESTAMP, new DateTime());
    		}
    		String commandId = queue.dispatch(new CreateEventCommand( principal, bucketId, event));
            response().setHeader(LOCATION, String.format("/buckets/%s/%s", bucket.getId(), event.getId()));
            response().setHeader("Undo", String.format("/queue/%s", commandId));
            return created();
    	}
    }

    public static Result get(String bucketId, String eventId) {
		Bucket bucket = manager.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	Identity principal = new SecurityContext(ctx()).getPrincipal();
    	if (bucket.getPermission(principal) == Permission.NONE) {
    		return forbidden();
    	}
    	Event event = manager.findEvent(bucketId, eventId);
    	if (event == null) {
    		return notFound();
    	}
    	return ok(event.toJson());
    }

    public static Result delete(String bucketId, String eventId) {
    	Identity principal = new SecurityContext(ctx()).getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
    	Bucket bucket = manager.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getPermission(principal) != Permission.ALL) {
    		return forbidden();
    	}
    	Event event = manager.findEvent(bucket.getId(), eventId);
    	if (event == null) {
    		return notFound();
    	}
    	String commandId = queue.dispatch(new DeleteEventCommand(principal, bucketId, event));
        response().setHeader("Undo", String.format("/queue/%s", commandId));
    	return noContent();
    }
}
