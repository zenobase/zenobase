package com.zenobase.tasks.foursquare;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class FoursquareTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(new FoursquareTaskManager(newCredentialsManager()), Nodes.newObject("marker", "2014-01-27"));
	}

	@Override
	protected FoursquareCredentialsManager newCredentialsManager() {
		return new FoursquareCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
