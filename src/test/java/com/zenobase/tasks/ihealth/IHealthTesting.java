package com.zenobase.tasks.ihealth;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class IHealthTesting extends TaskTestingSupport {

	private final String sc = System.getProperty("oauth.sc");

	private String getSV(String apiName) {
		return System.getProperty("oauth.sv." + apiName);
	}

	@Test
	@Ignore
	public void testWeight() {
		run(new IHealthWeightTaskManager(newCredentialsManager(), getSV("weight")), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("tag", "Weight")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	@Ignore
	public void testGlucose() {
		run(new IHealthGlucoseTaskManager(newCredentialsManager(), getSV("glucose")), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("tag", "Glucose")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	@Ignore
	public void testCardio() {
		run(new IHealthCardioTaskManager(newCredentialsManager(), getSV("bp"), getSV("spo2")), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("tag", "Cardio")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	@Ignore
	public void testFood() {
		run(new IHealthFoodTaskManager(newCredentialsManager(), getSV("food")), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("tag", "Meal")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	@Ignore
	public void testSleep() {
		run(new IHealthSleepTaskManager(newCredentialsManager(), getSV("sleep")), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("tag", "Sleep")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	@Ignore
	public void testSteps() {
		run(new IHealthStepsTaskManager(newCredentialsManager(), getSV("activity")), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("tag", "Steps")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	public void testActivities() {
		run(new IHealthActivitiesTaskManager(newCredentialsManager(), getSV("sport")), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("tag", "Activity")
			.put("timezone", "America/Los_Angeles"));
	}

	@Override
	protected IHealthCredentialsManager newCredentialsManager() {
		return new IHealthCredentialsManager(repository, apiKey, apiSecret, sc, callbackUrl);
	}
}
