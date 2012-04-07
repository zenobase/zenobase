package controllers;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;

import models.Bucket;
import models.Event;
import models.Permission;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;
import search.EventSearch;
import models.Identity;
import services.BucketManager;
import services.CommandQueue;
import services.IndexManager;

import commands.CreateEventCommand;
import commands.DeleteEventCommand;
import commands.GenerateRandomEventsCommand;
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
		Identity identity = Identities.in(ctx()).get();
		return identity != null ? find(bucketId, identity) : unauthorized(); 
    }

	private static Result find(String bucketId, Identity identity) {
		Bucket bucket = manager.findBucket(bucketId);
    	return bucket != null ? find(bucket, identity) : notFound();
    }

	private static Result find(Bucket bucket, Identity identity) {
    	return bucket.getPermission(identity) != Permission.NONE ? ok(new EventSearch()
			.addWidgets(request().queryString().get("w"))
			.addFilters(request().queryString().get("q"))
			.execute(node.getIndex(bucket.getId()))) : forbidden();
    }

	@BodyParser.Of(value = BodyParser.Json.class, maxLength = 1000)
	public static Result post(String bucketId) {
		
		Identity identity = Identities.in(ctx()).get();
		if (identity == null) {
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
    	if (bucket.getPermission(identity) != Permission.ALL) {
    		return forbidden();
    	}
    	if (body.has("random")) {
    		String commandId = queue.execute(new GenerateRandomEventsCommand(identity, manager, bucketId, body.get("random").asInt()));
            response().setHeader(LOCATION, String.format("/buckets/%s/", bucket.getId()));
            response().setHeader("Undo", String.format("/queue/%s", commandId));
            return created();
    	}
    	else {
    		Event event = new Event(body);
			event.setValue(Event.ID, Generator.id());
			event.setValue(Event.AUTHOR, identity);
    		if (!event.contains(Event.TIMESTAMP)) {
    			event.addValue(Event.TIMESTAMP, new DateTime());
    		}
    		String commandId = queue.execute(new CreateEventCommand(manager, identity, bucketId, event));
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
    	Identity identity = Identities.in(ctx()).get();
		return identity != null ? delete(bucketId, eventId, identity) : unauthorized();
    }

    private static Result delete(String bucketId, String eventId, Identity identity) {
    	Bucket bucket = manager.findBucket(bucketId);
    	return bucket != null ? delete(bucket, eventId, identity) : notFound();
    }

    private static Result delete(Bucket bucket, String eventId, Identity identity) {
    	if (bucket.getPermission(identity) != Permission.ALL) {
    		return forbidden();
    	}
    	Event event = manager.findEvent(bucket.getId(), eventId);
    	return event != null ? delete(bucket.getId(), event, identity) : notFound();
    }

    private static Result delete(String bucketId, Event event, Identity identity) {
    	String commandId = queue.execute(new DeleteEventCommand(manager, identity, bucketId, event));
        response().setHeader("Undo", String.format("/queue/%s", commandId));
    	return noContent();
    }
}
