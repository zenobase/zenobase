package com.zenobase.tasks.trakt;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class TraktTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(new TraktTaskManager(newCredentialsManager()), Nodes.newObject("marker", "2015-04-04"));
	}

	@Override
	protected TraktCredentialsManager newCredentialsManager() {
		return new TraktCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
