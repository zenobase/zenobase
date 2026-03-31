package com.zenobase.mail;

import com.google.common.base.Preconditions;

import com.zenobase.controllers.PasswordResetKey;
import com.zenobase.models.User;

public class PasswordResetMailer {

	private final Mailer mailer;
	private final String hostname;

	public PasswordResetMailer(Mailer mailer, String hostname) {
		this.mailer = mailer;
		this.hostname = hostname;
	}

	public void send(User user) {
		Preconditions.checkNotNull(user.getEmail());
		var key = new PasswordResetKey(user);
		String text = """
			Account:

			%s

			Follow the following link to reset your password:

			%s/#/users/%s/reset?key=%s&expires=%s

			If this was a mistake, just ignore this email and nothing will happen.
			""".formatted(user.getName(), hostname, user.getName(), key.getKey(), key.getExpirationToken());
		mailer.send(new Message(user.getEmail(), "Your Zenobase Password", text));
	}
}
