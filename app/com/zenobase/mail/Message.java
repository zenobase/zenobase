package com.zenobase.mail;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class Message {

	private final String to;
	private final String subject;
	private final String text;

	public Message(String to, String subject, String text) {
		Preconditions.checkNotNull(to);
		Preconditions.checkNotNull(subject);
		Preconditions.checkNotNull(text);
		this.to = to;
		this.subject = subject;
		this.text = text;
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
		return that instanceof Message &&
			equals((Message) that);
	}

	private boolean equals(Message that) {
		return to.equals(that.getTo()) &&
			subject.equals(that.getSubject()) &&
			text.equals(that.getText());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(to, subject, text);
	}
}
