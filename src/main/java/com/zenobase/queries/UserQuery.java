package com.zenobase.queries;

import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.repositories.QuerySupport;
import org.joda.time.DateTime;

public class UserQuery extends QuerySupport {

	public UserQuery principalEqualTo(Identity principal) {
		equalTo(User.ID, principal.id());
		return this;
	}

	public UserQuery isSuperuser(boolean b) {
		equalTo(User.SUPERUSER, b);
		return this;
	}

	public UserQuery isVerified(boolean b) {
		// VERIFIED is a default-false field; matching `false` should also include users with no value set.
		if (b) {
			equalTo(User.VERIFIED, true);
		} else {
			notEqualTo(User.VERIFIED, true);
		}
		return this;
	}

	public UserQuery isSuspended(boolean b) {
		// SUSPENDED is a default-false field; matching `false` should also include users with no value set.
		if (b) {
			equalTo(User.SUSPENDED, true);
		} else {
			notEqualTo(User.SUSPENDED, true);
		}
		return this;
	}

	public UserQuery createdBefore(DateTime time) {
		lessThan(User.CREATED, time);
		return this;
	}

	public UserQuery queryString(String query) {
		super.queryString(query, User.ID.getName());
		return this;
	}
}
