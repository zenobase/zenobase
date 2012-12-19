package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.EventRepository;

@With(Timed.class)
public class TagController extends ControllerSupport {

	private final BucketRepository buckets;
	private final EventRepository events;

	@Inject
	public TagController(AuthorizationContext security, BucketRepository buckets, EventRepository events) {
		super(security);
		this.buckets = buckets;
		this.events = events;
	}

	public Result get(String bucketId) {
		Authorization auth = getCurrentAuthorization();
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			return notFound();
		}
    	if (!bucket.hasRole(auth, Role.VIEWER)) {
    		return auth == null ? unauthorized() : forbidden();
    	}
    	return ok(Nodes.newArray(events.terms(bucketId, Event.TAG.getName())));
    }
}
