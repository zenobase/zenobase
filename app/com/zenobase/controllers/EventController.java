package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;
import play.mvc.With;

import com.zenobase.commands.DeleteEventCommand;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandQueue;

@With(Timed.class)
public class EventController extends ControllerSupport {

	@Inject
	static BucketRepository repository;

	@Inject
	static CommandQueue queue;

    public static Result get(String bucketId, String eventId) {
    	Identity principal = auth.getPrincipal();
    	if (principal == null) {
    		return unauthorized();
    	}
		Bucket bucket = repository.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getPermission(principal) == Permission.NONE) {
    		return forbidden();
    	}
    	Event event = repository.findEvent(bucketId, eventId);
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
    	Bucket bucket = repository.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getPermission(principal) != Permission.ALL) {
    		return forbidden();
    	}
    	Event event = repository.findEvent(bucket.getId(), eventId);
    	if (event == null) {
    		return notFound();
    	}
    	String commandId = queue.dispatch(new DeleteEventCommand(principal, bucketId, event));
    	return ok(receipt(commandId));
    }
}
