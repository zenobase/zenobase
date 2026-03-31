package com.zenobase.services;

import com.zenobase.models.Identity;
import com.zenobase.models.User;

public class UserQuery extends QuerySupport {

	public UserQuery principalEqualTo(Identity principal) {
		equalTo(User.ID, principal.id());
		return this;
	}

	public UserQuery isSuperuser(boolean b) {
		equalTo(User.SUPERUSER, b);
		return this;
	}

	public UserQuery queryString(String query) {
		super.queryString(query, User.ID.getName());
		return this;
	}
}
