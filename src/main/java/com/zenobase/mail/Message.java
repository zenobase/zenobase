package com.zenobase.mail;

import com.google.common.base.Preconditions;

public record Message(String to, String subject, String text) {

	public Message(String to, String subject, String text) {
		this.to = Preconditions.checkNotNull(to);
		this.subject = Preconditions.checkNotNull(subject);
		this.text = Preconditions.checkNotNull(text);
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Message m && to.equals(m.to()) && subject.equals(m.subject()) && text.equals(m.text());
	}
}
