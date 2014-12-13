package com.zenobase.tasks.ihealth;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class IHealthTesting extends TaskTestingSupport {

	@Test
	@Ignore
	public void testWeight() {
		run(new IHealthWeightTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("tag", "Weight")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	@Ignore
	public void testGlucose() {
		run(new IHealthGlucoseTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("tag", "Glucose")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	@Ignore
	public void testCardio() {
		run(new IHealthCardioTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("tag", "Cardio")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	@Ignore
	public void testFood() {
		run(new IHealthFoodTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("tag", "Meal")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	@Ignore
	public void testSleep() {
		run(new IHealthSleepTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("tag", "Sleep")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	@Ignore
	public void testSteps() {
		run(new IHealthStepsTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("tag", "Steps")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	public void testActivities() {
		run(new IHealthActivitiesTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("tag", "Activity")
			.put("timezone", "America/Los_Angeles"));
	}

	@Override
	protected IHealthCredentialsManager newCredentialsManager() {
		return new IHealthCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
