package com.zenobase.mail;

import java.util.Objects;

import com.google.common.base.Preconditions;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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
		send(Objects.requireNonNull(user.getName()), Objects.requireNonNull(user.getEmail()));
	}

	public void send(String username, String email) {
		var key = new EmailVerificationKey(username, email);
		Preconditions.checkNotNull(email);
		String text = """
			Account:

			%s

			Please verify your email address by opening the following link:

			%s/#/users/%s/verify?key=%s

			Thanks!
			""".formatted(username, hostname, username, key.getKey());

		mailer.send(new Message(email, "Your Zenobase Account", text));
	}
}
