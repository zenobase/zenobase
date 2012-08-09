package com.zenobase.io;

import play.mvc.Results.Chunks;
import play.mvc.Results.Chunks.Out;
import com.google.common.base.Joiner;

import com.zenobase.common.Callback;
import com.zenobase.models.User;

public class UserPrinter implements Callback<User> {

	private final Chunks.Out<String> out;

	public UserPrinter(Out<String> out) {
		this.out = out;
	}

	@Override
	public void call(User user) {
		out.write(toString(user));
	}

	private String toString(User user) {
		return Joiner.on('\t').join(user.getId(), user.getName(), user.getEmail(),
			user.isVerified() ? "verified" : "not verified",
			user.getCreated(), user.isSuspended() ? "suspended" : "active", "\n");
	}
}
