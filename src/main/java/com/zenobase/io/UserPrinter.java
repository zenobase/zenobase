package com.zenobase.io;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.google.common.base.Strings;
import com.zenobase.common.Callback;
import com.zenobase.models.User;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;

public class UserPrinter implements Callback<User> {

	private static final String[] HEADER = {
		"id",
		"name",
		"email",
		"external_id",
		"verified",
		"suspended",
		"optedout",
		"superuser",
		"quota",
		"created",
	};

	private final SequenceWriter writer;

	public UserPrinter(Writer out) throws IOException {
		writer = Csv.newWriter(out);
		writer.write(HEADER);
	}

	@Override
	public void call(User user) {
		try {
			writer.write(toRow(user));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void flush() throws IOException {
		writer.flush();
	}

	private static String[] toRow(User user) {
		return new String[] {
			Strings.nullToEmpty(user.getId()),
			Strings.nullToEmpty(user.getName()),
			Strings.nullToEmpty(user.getEmail()),
			Strings.nullToEmpty(user.getExternalId()),
			String.valueOf(user.isVerified()),
			String.valueOf(user.isSuspended()),
			String.valueOf(user.isOptedOut()),
			String.valueOf(user.isSuperuser()),
			user.getQuota() == null ? "" : user.getQuota().toString(),
			user.getCreated() == null ? "" : user.getCreated().toString(),
		};
	}
}
