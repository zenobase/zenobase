package com.zenobase.tasks.trakt;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;
import org.junit.jupiter.api.Test;

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
