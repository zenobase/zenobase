package com.zenobase.mail;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Preconditions;

import com.zenobase.controllers.EmailVerificationKey;
import com.zenobase.models.User;

public class VerificationMailer {

	private final Mailer mailer;
	private final String hostname;

	@Inject
	public VerificationMailer(Mailer mailer, @Named("hostname") String hostname) {
		this.mailer = mailer;
		this.hostname = hostname;
	}

	public void send(User user) {
		send(user.getName(), user.getEmail());
	}

	public void send(String username, String email) {
		EmailVerificationKey key = new EmailVerificationKey(username, email);
		Preconditions.checkNotNull(email);
		String text =
			"Account:\n\n" +
			"  " + username + "\n\n" +
			"Please verify your email address by opening the following link:\n\n" +
			"  " + hostname + "/#/users/" + username + "/verify?key=" + key.getKey() + "\n\n" +
			"Thanks!\n\n" +
			"P.S. For regular updates, follow us on Twitter <https://twitter.com/zenobase>, " +
			"Google+ <https://plus.google.com/+Zenobase> or Facebook <https://www.facebook.com/zenobase>.";

		mailer.send(new Message(email, "Your Zenobase Account", text));
	}
}
