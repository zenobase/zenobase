package com.zenobase.tasks;

import org.scribe.model.Token;

public class TwitterTask extends OAuthTask {

	public TwitterTask() {

	}

	public TwitterTask(String id, Token accessToken) {
		super(id, accessToken);
	}
}
