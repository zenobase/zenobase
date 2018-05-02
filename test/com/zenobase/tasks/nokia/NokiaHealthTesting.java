package com.zenobase.tasks.nokia;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class NokiaHealthTesting extends TaskTestingSupport {

	@Test
	public void testWeight() {
		run(new NokiaHealthWeightTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "weight")
			.put("unit", "lb")
			.put("timezone", "America/Los_Angeles")
			.put("marker", "2013-11-01"));
	}

	@Test
	@Ignore
	public void testSteps() {
		run(new NokiaHealthStepsTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "walk")
			.put("unit", "mi")
			.put("marker", "2013-10-01"));
	}

	@Test
	@Ignore
	public void testSleep() {
		run(new NokiaHealthSleepTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "Sleep")
			.put("marker", "2014-03-01T00:00:00Z")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	@Ignore
	public void testCardio() {
		run(new NokiaHealthCardioTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "cardio")
			.put("timezone", "America/Los_Angeles")
			.put("marker", "2014-04-16"));
	}

	@Override
	protected NokiaHealthCredentialsManager newCredentialsManager() {
		return new NokiaHealthCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
