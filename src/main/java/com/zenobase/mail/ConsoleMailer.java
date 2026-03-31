package com.zenobase.mail;

import jakarta.inject.Inject;
import jakarta.inject.Named;

public class ConsoleMailer implements Mailer {

	private final String from;

	@Inject
	public ConsoleMailer(@Named("mail.from") String from) {
		this.from = from;
	}

	@Override
	public void send(Message message) {
		System.out.printf("""
				--
				From: %s
				To: %s
				Subject: %s

				%s
				--
				%n""", from, message.to(), message.subject(), message.text());
	}
}
