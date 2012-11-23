package com.zenobase.tasks;

import org.scribe.model.Token;

import com.zenobase.common.Generator;

public abstract class OAuthTask { // TODO extend DomainNode

	private final String id;

	private Token token;

	protected OAuthTask() {
		this(Generator.id(), null);
	}

	protected OAuthTask(String id, Token token) {
		this.id = id;
		this.token = token;
	}

	public String getId() {
		return id;
	}

	public Token getToken() {
		return token;
	}

	public void setToken(Token token) {
		this.token = token;
	}
}
