package controllers;

import javax.inject.Inject;

import models.Bucket;
import models.Event;
import models.Identity;
import models.Permission;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;
import search.EventSearch;
import services.BucketManager;
import services.CommandQueue;
import services.IndexManager;

import commands.CreateEventCommand;
import commands.DeleteEventCommand;
import commands.RandomEventsCommandBuilder;
import common.Generator;
import common.Identities;

@With(Timed.class)
public class EventController extends ControllerSupport {

	@Inject
	static BucketManager manager;

	@Inject
	static IndexManager node;

	@Inject
	static CommandQueue queue;

	public static Result find(String bucketId) {
		Identity principal = Identities.in(ctx()).get();
		return principal != null ? find(bucketId, principal) : unauthorized(); 
    }

	private static Result find(String bucketId, Identity principal) {
		Bucket bucket = manager.findBucket(bucketId);
    	return bucket != null ? find(bucket, principal) : notFound();
    }

	private static Result find(Bucket bucket, Identity principal) {
    	return bucket.getPermission(principal) != Permission.NONE ? ok(new EventSearch()
			.addWidgets(request().queryString().get("w"))
			.addFilters(request().queryString().get("q"))
			.execute(node.getIndex(bucket.getId()))) : forbidden();
    }

	@BodyParser.Of(value = BodyParser.Json.class, maxLength = 1000)
	public static Result post(String bucketId) {
		
		Identity principal = Identities.in(ctx()).get();
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
    	if (bucket.getPermission(Identities.in(ctx()).get()) == Permission.NONE) {
    		return forbidden();
    	}
    	Event event = manager.findEvent(bucketId, eventId);
    	if (event == null) {
    		return notFound();
    	}
    	return ok(event.toJson());
    }

    public static Result delete(String bucketId, String eventId) {
    	Identity principal = Identities.in(ctx()).get();
		return principal != null ? delete(bucketId, eventId, principal) : unauthorized();
    }

    private static Result delete(String bucketId, String eventId, Identity principal) {
    	Bucket bucket = manager.findBucket(bucketId);
    	return bucket != null ? delete(bucket, eventId, principal) : notFound();
    }

    private static Result delete(Bucket bucket, String eventId, Identity principal) {
    	if (bucket.getPermission(principal) != Permission.ALL) {
    		return forbidden();
    	}
    	Event event = manager.findEvent(bucket.getId(), eventId);
    	return event != null ? delete(bucket.getId(), event, principal) : notFound();
    }

    private static Result delete(String bucketId, Event event, Identity principal) {
    	String commandId = queue.dispatch(new DeleteEventCommand(principal, bucketId, event));
        response().setHeader("Undo", String.format("/queue/%s", commandId));
    	return noContent();
    }
}
