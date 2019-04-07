package com.zenobase.tasks.withings;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class WithingsHealthTesting extends TaskTestingSupport {

	@Test
	public void testWeight() {
		run(new WithingsHealthWeightTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "weight")
			.put("unit", "lb")
			.put("timezone", "America/Los_Angeles")
			.put("marker", "2013-11-01"));
	}

	@Test
	@Ignore
	public void testSteps() {
		run(new WithingsHealthStepsTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "walk")
			.put("unit", "mi")
			.put("marker", "2013-10-01"));
	}

	@Test
	@Ignore
	public void testSleep() {
		run(new WithingsHealthSleepTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "Sleep")
			.put("marker", "2014-03-01T00:00:00Z")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	@Ignore
	public void testCardio() {
		run(new WithingsHealthCardioTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "cardio")
			.put("timezone", "America/Los_Angeles")
			.put("marker", "2014-04-16"));
	}

	@Override
	protected WithingsHealthCredentialsManager newCredentialsManager() {
		return new WithingsHealthCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
