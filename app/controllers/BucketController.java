package controllers;

import javax.inject.Inject;

import models.Bucket;
import models.Event;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;
import search.EventSearch;
import secure.Identity;
import services.BucketManager;
import services.CommandQueue;
import services.NodeManager;

import commands.CreateEventCommand;
import commands.DeleteBucketCommand;
import commands.GenerateRandomEventsCommand;

@With(Timed.class)
public class BucketController extends ControllerSupport {

	@Inject
	static CommandQueue queue;

	@Inject
	static NodeManager node;

	@Inject
	static BucketManager buckets;

	public static Result get(String bucketId) {
		Bucket bucket = buckets.findBucket(bucketId, SecurityController.identity(false));
    	return bucket != null ? get(bucket) : notFound();
    }

	private static Result get(Bucket bucket) {
    	return ok(new EventSearch(bucket)
			.addWidgets(request().queryString().get("w"))
			.addFilters(request().queryString().get("q"))
			.execute(node.getIndex(bucket.getId())));
    }

	@BodyParser.Of(value = BodyParser.Json.class, maxLength = 1000)
	public static Result post(String bucketId) {
		
		Identity identity = SecurityController.identity(false);
		if (identity == null) {
			return forbidden();
		}
		ObjectNode body = (ObjectNode) request().body().asJson();
		if (body == null) {
			return badRequest();
		}
    	Bucket bucket = buckets.findBucket(bucketId, identity);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (!"owner".equals(bucket.getRole())) {
    		return forbidden();
    	}
    	if (body.has("random")) {
    		String commandId = queue.execute(new GenerateRandomEventsCommand(identity, bucket, body.get("random").asInt()));
            response().setHeader(LOCATION, String.format("/buckets/%s/", bucket.getId()));
            response().setHeader("Undo", String.format("/queue/%s", commandId));
            return created();
    	}
    	else {
    		Event event = Event.newEvent(bucket.getId(), body);
    		if (!event.contains(Event.DATE_TIME)) {
    			event.add(Event.DATE_TIME, new DateTime());
    		}
    		String commandId = queue.execute(new CreateEventCommand(bucket, event));
            response().setHeader(LOCATION, String.format("/buckets/%s/%s", bucket.getId(), event.getId()));
            response().setHeader("Undo", String.format("/queue/%s", commandId));
            return created();
    	}
    }

    public static Result delete(String bucketId) {
    	Identity identity = SecurityController.identity(false);
		return identity != null ? delete(bucketId, identity) : forbidden();
    }

    private static Result delete(String bucketId, Identity identity) {
    	Bucket bucket = buckets.findBucket(bucketId, identity);
    	return bucket != null ? delete(bucket) : notFound();
    }

    private static Result delete(Bucket bucket) {
    	if (!"owner".equals(bucket.getRole())) {
    		return forbidden();
    	}
    	queue.execute(new DeleteBucketCommand(buckets, bucket));
    	return noContent();
    }
}
