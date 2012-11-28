package com.zenobase.tasks;

import org.junit.Assume;
import org.junit.Before;
import org.scribe.model.Token;

import com.zenobase.common.Generator;
import com.zenobase.models.Identity;

public class TaskTestSupport {

	protected final Identity principal = new Identity();
	protected final String bucketId = Generator.id();
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
