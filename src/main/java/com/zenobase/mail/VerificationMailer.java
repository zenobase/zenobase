package com.zenobase.mail;

import jakarta.inject.Inject;
import jakarta.inject.Named;

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
		var key = new EmailVerificationKey(username, email);
		Preconditions.checkNotNull(email);
		String text =
			"Account:\n\n" +
			"  " + username + "\n\n" +
			"Please verify your email address by opening the following link:\n\n" +
			"  " + hostname + "/#/users/" + username + "/verify?key=" + key.getKey() + "\n\n" +
			"Thanks!\n";

		mailer.send(new Message(email, "Your Zenobase Account", text));
	}
}
