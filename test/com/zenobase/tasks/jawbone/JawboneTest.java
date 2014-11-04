package com.zenobase.tasks.jawbone;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestSupport;

public class JawboneTest extends TaskTestSupport {

	@Test
	@Ignore
	public void testSteps() {
		run(new JawboneStepsTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "steps")
			.put("marker", "2014-01-01")
			.put("hourly", false)
			.put("metric", true));
	}

	@Test
	@Ignore
	public void testSleep() {
		run(new JawboneSleepTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "sleep")
			.put("marker", "2014-01-01"));
	}

	@Test
	@Ignore
	public void testFood() {
		run(new JawboneFoodTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "Meal")
			.put("marker", "2014-01-01"));
	}

	@Override
	protected JawboneCredentialsManager newCredentialsManager() {
		return new JawboneCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
