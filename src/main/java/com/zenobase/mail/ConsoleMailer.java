package com.zenobase.mail;

public class ConsoleMailer implements Mailer {

	private final String from;

	public ConsoleMailer(String from) {
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
