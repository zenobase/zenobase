package com.zenobase.tasks.fitbit;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class FitbitTesting extends TaskTestingSupport {

	@Test
	public void testSteps() {
		run(new FitbitStepsTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2015-10-01")
			.put("tag", "walk")
			.put("hourly", false));
	}

	@Test
	@Ignore
	public void testSleep() {
		run(new FitbitSleepTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2013-11-01")
			.put("tag", "zzz"));
	}

	@Test
	@Ignore
	public void testActivities() {
		run(new FitbitActivitiesTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2015-07-25T00:00:00Z"));
	}

	@Test
	@Ignore
	public void testWeight() {
		run(new FitbitWeightTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-10-01")
			.put("tag", "weight"));
	}

	@Test
	@Ignore
	public void testFood() {
		run(new FitbitFoodTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2013-01-01")
			.put("tag", "eat"));
	}

	@Test
	@Ignore
	public void testCardio() {
		run(new FitbitCardioTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2015-07-28")
			.put("tag", "hr")
			.put("hourly", true));
	}

	@Override
	protected FitbitCredentialsManager newCredentialsManager() {
		return new FitbitCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
