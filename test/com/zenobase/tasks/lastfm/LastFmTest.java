package com.zenobase.tasks.lastfm;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestSupport;

public class LastFmTest extends TaskTestSupport {

	@Test
	public void test() {
		run(new LastFmTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "track")
			.put("timezone", "America/Los_Angeles")
			.put("marker", "2014-05-01T17:35:45.000Z"));
	}

	@Override
	protected LastFmCredentialsManager newCredentialsManager() {
		return new LastFmCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
