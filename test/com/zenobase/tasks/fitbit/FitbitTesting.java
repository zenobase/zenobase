package com.zenobase.tasks.fitbit;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class FitbitTesting extends TaskTestingSupport {

	@Test
	@Ignore
	public void testSteps() {
		run(new FitbitStepsTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2015-01-01")
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
	public void testActivities() {
		run(new FitbitActivitiesTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2015-05-11T21:35:00"));
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

	@Override
	protected FitbitCredentialsManager newCredentialsManager() {
		return new FitbitCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
