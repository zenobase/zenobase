package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.commands.CreateBucketCommand;
import com.zenobase.io.BucketPrinter;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class BucketListController extends ControllerSupport {

	@Inject
	static CommandDispatcher dispatcher;

	@Inject
	static BucketRepository buckets;

	@Inject
	static UserRepository users;

    public static Result find(String identity, int offset, int limit) {
    	Identity principal = auth.getPrincipal();
    	if (principal == null) {
    		return unauthorized();
    	}
    	if (identity != null) {
    		return find(principal, new Identity(identity), offset, limit);
    	}
        return find(principal, offset, limit);
    }

    private static Result find(Identity principal, Identity identity, int offset, int limit) {
    	if (!identity.equals(principal)) {
    		return forbidden();
    	}
        return ok(buckets.findBuckets(identity, offset, limit).toJson());
    }

    private static Result find(Identity principal, int offset, int limit) {
    	if (!users.isSuperuser(principal)) {
    		return forbidden();
    	}
    	if (limit == Integer.MAX_VALUE) {
    		return findAll();
    	}
        return ok(buckets.findBuckets(offset, limit).toJson());
    }

    private static Result findAll() {
    	Chunks<String> chunks = new StringChunks() {
			@Override
			public void onReady(final Out<String> out) {
				buckets.findBuckets(new BucketPrinter(out));
		    	out.close();
			}
		};
        return ok(chunks);
	}

    @BodyParser.Of(BodyParser.Json.class)
    public static Result post() {
    	Identity principal = auth.getPrincipal(true);
		CreateBucketForm form = new CreateBucketForm(body());
		Bucket bucket = form.getId() != null ? new Bucket(form.getId()) : new Bucket();
		bucket.setLabel(form.getLabel());
		bucket.setDescription(form.getDescription());
		bucket.addPermission(principal, Permission.ALL);
		if (!bucket.valid()) {
			return badRequest("not valid");
		}
    	String commandId = dispatcher.dispatch(new CreateBucketCommand(principal, bucket));
        response().setHeader(LOCATION, com.zenobase.controllers.routes.BucketController.get(bucket.getId()).toString());
        return created(receipt(commandId));
    }
}
