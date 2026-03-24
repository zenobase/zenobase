package com.zenobase.controllers;

import jakarta.inject.Inject;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.EventRepository;

public class TagController extends ControllerSupport {

	private final BucketRepository buckets;
	private final EventRepository events;

	@Inject
	public TagController(AuthorizationContext security, BucketRepository buckets, EventRepository events) {
		super(security);
		this.buckets = buckets;
		this.events = events;
	}

	public void get(ServerRequest req, ServerResponse res) {
		String bucketId = req.path().pathParameters().get("bucketId");
		Authorization auth = getCurrentAuthorization(req);
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			sendNotFound(res);
			return;
		}
    	if (!bucket.hasRole(auth, Role.VIEWER)) {
    		if (auth == null) {
    			sendUnauthorized(res);
    		} else {
    			sendForbidden(res);
    		}
    		return;
    	}
    	sendOk(res, Nodes.newArray(events.terms(bucketId, Event.TAG.getName())));
    }
}
