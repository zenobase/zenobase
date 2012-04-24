package com.zenobase.controllers;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import com.zenobase.common.BCrypt;
import com.zenobase.models.User;
import com.zenobase.services.Mailer;
import com.zenobase.services.Message;

public class PasswordResetMailer {

	private final Mailer mailer;
	private final String hostname;

	@Inject
	public PasswordResetMailer(Mailer mailer, @Named("hostname") String hostname) {
		this.mailer = mailer;
		this.hostname = hostname;
	}

	public void send(User user) {
		Preconditions.checkNotNull(user.getEmail());
		String expires = Long.toString(new DateTime(DateTimeZone.UTC).plusDays(1).getMillis(), 36);
		String text =
			"Account:\n\n" +
			"  " + user.getName() + "\n\n" +
			"Follow the following link to reset your password:\n\n" +
			"  <" + hostname + "/#/users/" + user.getName() + "/reset?key=" + BCrypt.hashpw(toString(user, expires), BCrypt.gensalt()) + "&expires=" + expires + ">\n\n" +
			"If this was a mistake, just ignore this email and nothing will happen.\n";
		mailer.send(new Message(user.getEmail(), "Your Zenobase Password", text));
	}

	public static String toString(User user, String expires) {
		return Joiner.on('|').join(user.getName(), user.getHashedPassword(), expires);
	}
}
