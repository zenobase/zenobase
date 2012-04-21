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
		Preconditions.checkNotNull(user.getEmail());
		String text =
			"Please verify your email address by opening the following link:\n\n" +
			"  <" + hostname + "/#/verify?h=" + BCrypt.hashpw(toString(user), BCrypt.gensalt()) + ">\n\n" +
			"Thanks!\n";
		mailer.send(new Message(user.getEmail(), "Your Zenobase Account", text));
	}

	public static String toString(User user) {
		return Joiner.on('|').join(user.getName(), user.getEmail());
	}
}
