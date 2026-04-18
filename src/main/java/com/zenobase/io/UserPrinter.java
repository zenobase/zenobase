package com.zenobase.io;

import com.google.common.base.Joiner;
import com.zenobase.common.Callback;
import com.zenobase.models.User;
import java.io.IOException;
import java.io.Writer;

public class UserPrinter implements Callback<User> {

	private final Writer out;

	public UserPrinter(Writer out) {
		this.out = out;
	}

	@Override
	public void call(User user) {
		try {
			out.write(toString(user));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String toString(User user) {
		return Joiner.on('\t').join(
			user.getId(),
			user.getName(),
			user.getEmail(),
			user.isOptedOut() ? "opted out" : "",
			user.isVerified() ? "verified" : "",
			user.getCreated(),
			user.isSuspended() ? "suspended" : "",
			"\n"
		);
	}
}
