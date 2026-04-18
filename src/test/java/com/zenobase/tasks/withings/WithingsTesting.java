package com.zenobase.tasks.withings;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class WithingsTesting extends TaskTestingSupport {

	@Test
	@Disabled
	public void testWeight() {
		run(
			new WithingsWeightTaskManager(newCredentialsManager()),
			Nodes.newObject()
				.put("tag", "weight")
				.put("unit", "lb")
				.put("timezone", "America/Los_Angeles")
				.put("marker", "2013-11-01")
		);
	}

	@Test
	@Disabled
	public void testSteps() {
		run(
			new WithingsStepsTaskManager(newCredentialsManager()),
			Nodes.newObject().put("tag", "walk").put("unit", "mi").put("marker", "2013-10-01")
		);
	}

	@Test
	@Disabled
	public void testSleep() {
		run(
			new WithingsSleepTaskManager(newCredentialsManager()),
			Nodes.newObject()
				.put("tag", "Sleep")
				.put("marker", "2014-03-01T00:00:00Z")
				.put("timezone", "America/Los_Angeles")
		);
	}

	@Test
	@Disabled
	public void testCardio() {
		run(
			new WithingsCardioTaskManager(newCredentialsManager()),
			Nodes.newObject().put("tag", "cardio").put("timezone", "America/Los_Angeles").put("marker", "2014-04-16")
		);
	}

	@Test
	public void testTemperature() {
		run(
			new WithingsTemperatureTaskManager(newCredentialsManager()),
			Nodes.newObject()
				.put("tag", "temp")
				.put("unit", "C")
				.put("timezone", "America/Los_Angeles")
				.put("marker", "2019-04-01")
		);
	}

	@Override
	protected WithingsCredentialsManager newCredentialsManager() {
		return new WithingsCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
