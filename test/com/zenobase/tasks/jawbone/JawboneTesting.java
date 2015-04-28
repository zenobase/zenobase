package com.zenobase.tasks.jawbone;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class JawboneTesting extends TaskTestingSupport {

	@Test
	@Ignore
	public void testActivities() {
		run(new JawboneActivitiesTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2011-01-01")
			.put("metric", true));
	}

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
			.put("marker", "2013-08-17"));
	}

	@Test
	@Ignore
	public void testFood() {
		run(new JawboneFoodTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "Meal")
			.put("marker", "2014-01-01"));
	}

	@Test
	@Ignore
	public void testWeight() {
		run(new JawboneWeightTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "Body")
			.put("metric", "Meal")
			.put("marker", "2014-01-01"));
	}

	@Test
	public void testMood() {
		run(new JawboneMoodTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "Mood")
			.put("marker", "2015-04-15"));
	}

	@Override
	protected JawboneCredentialsManager newCredentialsManager() {
		return new JawboneCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
