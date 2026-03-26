package com.zenobase.tasks.fitbit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class FitbitTesting extends TaskTestingSupport {

	@Test
	public void testSteps() {
		run(
				new FitbitStepsTaskManager(newCredentialsManager()),
				Nodes.newObject().put("marker", "2020-06-01").put("tag", "walk").put("hourly", false));
	}

	@Test
	@Disabled
	public void testSleep() {
		run(
				new FitbitSleepTaskManager(newCredentialsManager()),
				Nodes.newObject().put("marker", "2013-11-01").put("tag", "zzz"));
	}

	@Test
	@Disabled
	public void testActivities() {
		run(
				new FitbitActivitiesTaskManager(newCredentialsManager()),
				Nodes.newObject().put("marker", "2015-07-25T00:00:00Z"));
	}

	@Test
	@Disabled
	public void testWeight() {
		run(
				new FitbitWeightTaskManager(newCredentialsManager()),
				Nodes.newObject().put("marker", "2014-10-01").put("tag", "weight"));
	}

	@Test
	@Disabled
	public void testFood() {
		run(
				new FitbitFoodTaskManager(newCredentialsManager()),
				Nodes.newObject().put("marker", "2013-01-01").put("tag", "eat"));
	}

	@Test
	@Disabled
	public void testCardio() {
		run(
				new FitbitCardioTaskManager(newCredentialsManager()),
				Nodes.newObject().put("marker", "2015-07-28").put("tag", "hr").put("hourly", true));
	}

	@Test
	@Disabled
	public void testBurn() {
		run(
				new FitbitBurnTaskManager(newCredentialsManager()),
				Nodes.newObject().put("marker", "2016-04-25").put("tag", "burn").put("hourly", true));
	}

	@Override
	protected FitbitCredentialsManager newCredentialsManager() {
		return new FitbitCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
