package controllers;

import java.io.IOException;

import javax.inject.Inject;

import models.Bucket;
import models.Event;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import services.BucketManager;

import common.JsonPrinter;

@With(UserController.class)
public class EventController extends Controller {

	@Inject
	static BucketManager manager;

    public static void get(String bucketId, String eventId) throws IOException {
		Logger.info("Event: %s/%s", bucketId, eventId);
    	Bucket bucket = manager.findBucket(bucketId, Security.connected());
    	notFoundIfNull(bucket);
    	Event event = bucket.findEvent(eventId);
    	notFoundIfNull(event);    	
    	if ("json".equals(request.format)) {
        	new JsonPrinter(response.out).print(event.toJson());
    	}
    	else {
    		renderArgs.put("map", event.toMap());
    		renderTemplate("event.html", event, bucket);
    	}
    }
}
