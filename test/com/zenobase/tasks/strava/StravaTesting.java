package com.zenobase.tasks.strava;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class StravaTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(new StravaTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2015-06-01")
			.put("metric", "false"));
	}

	@Override
	protected StravaCredentialsManager newCredentialsManager() {
		return new StravaCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
