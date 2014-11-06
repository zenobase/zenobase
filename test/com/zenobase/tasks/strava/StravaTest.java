package com.zenobase.tasks.strava;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestSupport;

public class StravaTest extends TaskTestSupport {

	@Test
	public void test() {
		run(new StravaTaskManager(newCredentialsManager()), Nodes.newObject("marker", "2011-01-01").put("metric", "false"));
	}

	@Override
	protected StravaCredentialsManager newCredentialsManager() {
		return new StravaCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
