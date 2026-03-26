package com.zenobase.tasks.lastfm;

import org.junit.jupiter.api.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class LastFmTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(
				new LastFmTaskManager(newCredentialsManager()),
				Nodes.newObject()
						.put("tag", "track")
						.put("timezone", "America/Los_Angeles")
						.put("marker", "2014-12-10T17:35:45.000Z"));
	}

	@Override
	protected LastFmCredentialsManager newCredentialsManager() {
		return new LastFmCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
