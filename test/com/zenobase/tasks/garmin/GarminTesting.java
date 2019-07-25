package com.zenobase.tasks.garmin;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class GarminTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(new GarminActivitiesTaskManager(newCredentialsManager()), Nodes.newObject("marker", "2020-04-24T00:00:00-07:00"));
	}

	@Override
	protected GarminCredentialsManager newCredentialsManager() {
		return new GarminCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
