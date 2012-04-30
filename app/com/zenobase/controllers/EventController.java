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
import com.zenobase.json.IntegerField;
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

	private static final IntegerField RANDOM = new IntegerField("random");

	@Inject
	static BucketManager manager;

	@Inject
	static IndexManager node;

	@Inject
	static CommandQueue queue;

	public static Result find(String bucketId) {
		Identity principal = auth.getPrincipal();
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

	@BodyParser.Of(value = BodyParser.Json.class)
	public static Result post(String bucketId) {
		Identity principal = auth.getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
		ObjectNode body = body();
    	Bucket bucket = manager.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getPermission(principal) != Permission.ALL) {
    		return forbidden();
    	}
    	Integer random = RANDOM.getValue(body);
    	if (random != null) {
    		String commandId = queue.dispatch(new RandomEventsCommandBuilder(principal, bucketId).build(random));
            return ok(receipt(commandId));
    	}
    	else {
    		Event event = new Event(body);
			event.setValue(Event.ID, Generator.id());
			event.setValue(Event.AUTHOR, principal);
    		if (!event.contains(Event.TIMESTAMP)) {
    			event.addValue(Event.TIMESTAMP, new DateTime());
    		}
    		String commandId = queue.dispatch(new CreateEventCommand(principal, bucketId, event));
            response().setHeader(LOCATION, com.zenobase.controllers.routes.EventController.get(bucket.getId(), event.getId()).toString());
            return created(receipt(commandId));
    	}
    }

    public static Result get(String bucketId, String eventId) {
		Bucket bucket = manager.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	Identity principal = auth.getPrincipal();
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
    	Identity principal = auth.getPrincipal();
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
    	return ok(receipt(commandId));
    }
}
