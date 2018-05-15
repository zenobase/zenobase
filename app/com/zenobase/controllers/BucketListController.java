package com.zenobase.controllers;

import javax.inject.Inject;

import play.mvc.BodyParser;
import play.mvc.Result;

import com.zenobase.commands.CreateBucketCommand;
import com.zenobase.common.PartialList;
import com.zenobase.io.BucketPrinter;
import com.zenobase.models.Bucket;
import com.zenobase.models.BucketList;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketQuery;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.EventRepository;
import com.zenobase.services.SearchOrder;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;

public class BucketListController extends ControllerSupport {

	private final CommandDispatcher dispatcher;
	private final BucketRepository buckets;
	private final EventRepository events;
	private final UserRepository users;

	@Inject
    public BucketListController(AuthorizationContext auth, CommandDispatcher dispatcher,
    	BucketRepository buckets, EventRepository events, UserRepository users) {

		super(auth);
		this.dispatcher = dispatcher;
		this.buckets = buckets;
		this.events = events;
		this.users = users;
	}

	public Result findAll(String q, int offset, int limit) {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
		if (auth.getScope() != null) {
    		return forbidden();
		}
		if (!users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
    	if (limit == Integer.MAX_VALUE) {
    		return findAll();
    	}
    	BucketQuery query = new BucketQuery();
    	if (q != null) {
    		query = query.queryString(q);
    	}
    	return ok(BucketList.toJson(buckets.find(query, BucketQuery.DEFAULT_ORDER, offset, limit), events));
    }

    private Result findAll() {
    	Chunks<String> chunks = new StringChunks() {
			@Override
			public void onReady(Out<String> out) {
				buckets.findAll(new BucketPrinter(events, out));
		    	out.close();
			}
		};
        return ok(chunks);
	}

	public Result findByUser(String userId, String order, int offset, int limit, boolean labelsOnly, boolean includeArchived) {
		if (offset < 0 || offset > 1000) {
			return badRequest("expected offset in [0..1000]");
		}
		if (limit < 0 || limit > 100) {
			return badRequest("expected limit in [0..100]");
		}
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	if (auth.getScope() != null) {
    		return forbidden();
    	}
		Identity principal = new UserLookup(users).getIdentity(userId);
		if (principal == null) {
			return notFound("user not found");
		}
		if (!auth.getPrincipal().equals(principal) && !users.isSuperuser(auth.getPrincipal())) {
			return forbidden();
		}
		BucketQuery query = new BucketQuery().principalEqualTo(principal).includeArchived(includeArchived);
        PartialList<Bucket> found = buckets.find(query, SearchOrder.valueOf(order, Bucket.SCHEMA), offset, limit);
		return ok(labelsOnly ? BucketList.toJsonLabelsOnly(found) : BucketList.toJson(found, events));
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
		bucket.setWidgets(form.getWidgets());
		bucket.setAliases(form.getIncluded());
		bucket.addRole(auth.getPrincipal(), Role.OWNER);
		if (!bucket.valid()) {
			return badRequest("not valid");
		}
		if (!new AliasValidator(buckets).checkPermissions(bucket, auth)) {
			return badRequest("one or more aliases are invalid");
		}
    	String commandId = dispatcher.dispatch(new CreateBucketCommand(auth.getPrincipal(), bucket));
        response().setHeader(LOCATION, com.zenobase.controllers.routes.BucketController.get(bucket.getId(), false).toString());
		response().setHeader(COMMAND_ID, commandId);
        return created(bucket.toJson());
    }
}
