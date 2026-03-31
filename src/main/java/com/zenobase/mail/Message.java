package com.zenobase.mail;

import com.google.common.base.Preconditions;

public record Message(String to, String subject, String text) {

	public Message {
		Preconditions.checkNotNull(to);
		Preconditions.checkNotNull(subject);
		Preconditions.checkNotNull(text);
	}
}
