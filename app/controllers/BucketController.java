package controllers;

import java.io.IOException;

import javax.inject.Inject;

import models.Bucket;
import models.Event;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;

import com.google.common.base.Objects;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http.StatusCode;
import queries.BucketQuery;
import queries.BucketResult;
import services.BucketManager;
import services.CommandQueue;
import services.IndexManager;
import services.NodeManager;

import commands.CreateEventCommand;
import commands.DeleteBucketCommand;
import commands.GenerateRandomEventsCommand;
import common.RenderJackson;

public class BucketController extends Controller {

	@Inject
	static CommandQueue queue;

	@Inject
	static NodeManager node;

	@Inject
	static BucketManager buckets;

	public static void get(String bucketId) {
		Logger.info("Bucket: %s", bucketId);
    	Bucket bucket = buckets.findBucket(bucketId, AuthController.currentUser());
    	notFoundIfNull(bucket);
		Logger.info("Bucket: %s", bucket.getId());
    	IndexManager index = node.getIndex(bucketId);
    	int offset = Objects.firstNonNull(params.get("offset", Integer.class), Integer.valueOf(0));
    	int limit = Objects.firstNonNull(params.get("limit", Integer.class), Integer.valueOf(0));
    	String[] facets = Objects.firstNonNull(params.getAll("facet"), new String[0]);
    	String[] filters = Objects.firstNonNull(params.getAll("filter"), new String[0]);
    	BucketResult result = new BucketQuery(bucket).setOffset(offset).setLimit(limit)
			.addFacets(facets).addFilters(filters).execute(index);
    	renderJson(result.toJson());
    }

	public static void post(String bucketId, ObjectNode body) throws IOException {
		String user = AuthController.currentUser();
		if (user == null) {
			forbidden();
		}
		validation.required(bucketId);
    	validation.required(body);
    	if (validation.hasErrors()) {
    		Logger.warn("Rejected: %s", validation.errorsMap());
    		badRequest();
    	}
    	Logger.info("Content: %s", body);
    	Bucket bucket = buckets.findBucket(bucketId, user);
    	notFoundIfNull(bucket);
    	if (!"owner".equals(bucket.getRole())) {
    		forbidden();
    	}
    	if (body.has("random")) {
    		String commandId = queue.execute(new GenerateRandomEventsCommand(user, bucket, body.get("random").asInt()));
    		response.status = StatusCode.CREATED;
            response.setHeader("Location", String.format("/buckets/%s/", bucket.getId()));
            response.setHeader("Undo", String.format("/queue/%s", commandId));
    	}
    	else {
    		Event event = Event.newEvent(bucket.getId(), body);
    		if (!event.contains(Event.DATE_TIME)) {
    			event.add(Event.DATE_TIME, new DateTime());
    		}
    		String commandId = queue.execute(new CreateEventCommand(bucket, event));
    		response.status = StatusCode.CREATED;
            response.setHeader("Location", String.format("/buckets/%s/%s", bucket.getId(), event.getId()));
            response.setHeader("Undo", String.format("/queue/%s", commandId));
    	}
    }

    public static void delete(String bucketId) {
		Logger.info("Delete: %s", bucketId);
		String user = AuthController.currentUser();
		if (user == null) {
			forbidden();
		}
    	Bucket bucket = buckets.findBucket(bucketId, user);
    	notFoundIfNull(bucket);
    	if (!"owner".equals(bucket.getRole())) {
    		forbidden();
    	}
    	queue.execute(new DeleteBucketCommand(buckets, bucket));
    	response.status = StatusCode.NO_RESPONSE;
    }

	private static void renderJson(JsonNode object) {
		throw new RenderJackson(object);
	}
}
