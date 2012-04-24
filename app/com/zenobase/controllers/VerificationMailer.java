package com.zenobase.controllers;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import com.zenobase.common.BCrypt;
import com.zenobase.models.User;
import com.zenobase.services.Mailer;
import com.zenobase.services.Message;

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
		Preconditions.checkNotNull(email);
		String text =
			"Account:\n\n" +
			"  " + username + "\n\n" +
			"Please verify your email address by opening the following link:\n\n" +
			"  " + hostname + "/#/users/" + username + "/verify?key=" + BCrypt.hashpw(toString(username, email), BCrypt.gensalt()) + "\n\n" +
			"Thanks!\n";
		mailer.send(new Message(email, "Your Zenobase Account", text));
	}

	public static String toString(User user) {
		return toString(user.getName(), user.getEmail());
	}

	public static String toString(String username, String email) {
		return Joiner.on('\t').join(username, email);
	}
}
