package com.zenobase.mail;

import java.util.Objects;

import com.google.common.base.Preconditions;

public class Message {

	private final String to;
	private final String subject;
	private final String text;

	public Message(String to, String subject, String text) {
		this.to = Preconditions.checkNotNull(to);
		this.subject = Preconditions.checkNotNull(subject);
		this.text = Preconditions.checkNotNull(text);
	}

	public String getTo() {
		return to;
	}

	public String getSubject() {
		return subject;
	}

	public String getText() {
		return text;
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Message m
				&& to.equals(m.getTo())
				&& subject.equals(m.getSubject())
				&& text.equals(m.getText());
	}

	@Override
	public int hashCode() {
		return Objects.hash(to, subject, text);
	}
}
