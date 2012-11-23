package com.zenobase.tasks;

import org.junit.Assume;
import org.junit.Before;
import org.scribe.model.Token;

public class TaskTestSupport {

	protected final String apiKey = System.getProperty("oauth.apiKey");
	protected final String apiSecret = System.getProperty("oauth.apiSecret");
	protected final String callbackUrl = "https://zenobase.com/tasks/";

	@Before
	public void setUp() {
		Assume.assumeNotNull(apiKey);
		Assume.assumeNotNull(apiSecret);
	}

	protected Token getToken() {
		String token = System.getProperty("oauth.token");
		String secret = System.getProperty("oauth.secret", "");
		Assume.assumeNotNull(token);
		return new Token(token, secret);
	}
}
