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
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class BucketListController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final BucketRepository buckets;
	private final UserRepository users;

	@Inject
    public BucketListController(AuthorizationContext security, CommandDispatcher dispatcher,
    	BucketRepository buckets, UserRepository users) {

		super(security);
		this.dispatcher = dispatcher;
		this.buckets = buckets;
		this.users = users;
	}

	public Result find(String identity, int offset, int limit) {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null || auth.getScope() != null) {
    		return unauthorized();
    	}
    	if (identity != null) {
    		return find(auth.getPrincipal(), new Identity(identity), offset, limit);
    	}
        return find(auth.getPrincipal(), offset, limit);
    }

    private Result find(Identity principal, Identity identity, int offset, int limit) {
    	if (!(identity.equals(principal) || users.isSuperuser(principal))) {
    		return forbidden();
    	}
    	if (limit > 100) {
    		return badRequest("limit max 100");
    	}
        return ok(buckets.findBuckets(identity, offset, limit).toJson());
    }

    private Result find(Identity principal, int offset, int limit) {
    	if (!users.isSuperuser(principal)) {
    		return forbidden();
    	}
    	if (limit == Integer.MAX_VALUE) {
    		return findAll();
    	}
    	if (limit > 100) {
    		return badRequest("limit max 100");
    	}
        return ok(buckets.findBuckets(offset, limit).toJson());
    }

    private Result findAll() {
    	Chunks<String> chunks = new StringChunks() {
			@Override
			public void onReady(final Out<String> out) {
				buckets.findBuckets(new BucketPrinter(buckets, out));
		    	out.close();
			}
		};
        return ok(chunks);
	}

    @BodyParser.Of(BodyParser.Json.class)
    public Result post() {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null || auth.getScope() != null) {
    		return unauthorized();
    	}
		CreateBucketForm form = new CreateBucketForm(body());
		Bucket bucket = form.getId() != null ? new Bucket(form.getId()) : new Bucket();
		bucket.setLabel(form.getLabel());
		bucket.setDescription(form.getDescription());
		bucket.addPermission(auth.getPrincipal(), Permission.ALL);
		if (!bucket.valid()) {
			return badRequest("not valid");
		}
    	String commandId = dispatcher.dispatch(new CreateBucketCommand(auth.getPrincipal(), bucket));
        response().setHeader(LOCATION, com.zenobase.controllers.routes.BucketController.get(bucket.getId()).toString());
        return created(commandId);
    }
}
