package com.zenobase.controllers;

import javax.inject.Inject;

import org.joda.time.DateTime;
import play.mvc.Result;

import com.zenobase.io.UserPrinter;
import com.zenobase.models.UserList;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.UserQuery;
import com.zenobase.services.UserRepository;

public class UserListController extends ControllerSupport {

	private final UserRepository repository;

	@Inject
	public UserListController(AuthorizationContext security, UserRepository repository) {
		super(security);
		this.repository = repository;
	}

	public Result find(String q, int offset, int limit) {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null || auth.getScope() != null) {
    		return unauthorized();
    	}
    	if (!repository.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
    	if (limit == Integer.MAX_VALUE) {
    		return find();
    	}
    	UserQuery query = parseQuery(q);
    	if (query == null) {
    		return badRequest("Unsupported query: " + q);
    	}
        return ok(UserList.toJson(repository.find(query, offset, limit)));
    }

	private UserQuery parseQuery(String q) {
    	if ("-quota:*|created:(*..1M]".equals(q)) {
    		return new UserQuery().quotaIsNull().createdBefore(DateTime.now().minusMonths(1));
    	}
    	if (q != null) {
    		return null;
    	}
    	return new UserQuery();

	}
	private Result find() {
    	Chunks<String> chunks = new StringChunks() {
			@Override
			public void onReady(final Out<String> out) {
				repository.find(new UserPrinter(out));
		    	out.close();
			}
		};
        return ok(chunks);
	}
}
