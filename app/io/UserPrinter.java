package io;

import models.User;
import play.mvc.Results.Chunks;
import play.mvc.Results.Chunks.Out;

import com.google.common.base.Joiner;

public class UserPrinter {

	private final Chunks.Out<String> out;

	public UserPrinter(Out<String> out) {
		this.out = out;
	}

	public void print(User user) {
		out.write(toString(user));
	}

	private String toString(User user) {
		return Joiner.on('\t').join(user.getId(), user.getName(), user.getEmail(), user.getCreated(), "\n");
	}
}
