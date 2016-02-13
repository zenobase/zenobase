package com.zenobase.services;

import org.joda.time.DateTime;

import com.zenobase.models.Identity;
import com.zenobase.models.User;

public class UserQuery extends QuerySupport {

	public UserQuery principalEqualTo(Identity principal) {
		equalTo(User.ID, principal.getId());
		return this;
	}

	public UserQuery isSuperuser(boolean b) {
		equalTo(User.SUPERUSER, b);
		return this;
	}

	public UserQuery quotaIsNull() {
		isNull(User.QUOTA);
		return this;
	}

	public UserQuery createdBefore(DateTime time) {
		lessThan(User.CREATED, time);
		return this;
	}

	@Override
	public UserQuery queryString(String query) {
		super.queryString(query, User.ID.getName());
		return this;
	}
}
