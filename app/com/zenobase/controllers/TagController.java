package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.services.BucketRepository;

@With(Timed.class)
public class TagController extends ControllerSupport {

	private final BucketRepository buckets;

	@Inject
	public TagController(SecurityContext security, BucketRepository buckets) {
		super(security);
		this.buckets = buckets;
	}

	public Result get(String bucketId) {
		Identity principal = getSecurityContext().getPrincipal();
		Bucket bucket = buckets.findBucket(bucketId);
		if (bucket == null) {
			return notFound();
		}
    	if (bucket.getPermission(principal) == Permission.NONE) {
    		return principal == null ? unauthorized() : forbidden();
    	}
    	return ok(Nodes.newArray(buckets.terms(bucketId, Event.TAG.getName())));
    }
}
