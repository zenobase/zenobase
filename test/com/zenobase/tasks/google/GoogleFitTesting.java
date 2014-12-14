package com.zenobase.tasks.google;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class GoogleFitTesting extends TaskTestingSupport {

	@Test
	@Ignore
	public void testActivities() {
		run(new GoogleFitActivitiesTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-12-13")
			.put("timezone", "America/Los_Angeles")
			.put("metric", true)
			.put("derived", false));
	}

	@Test
	@Ignore
	public void testWeight() {
		run(new GoogleFitWeightTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-10-31")
			.put("timezone", "America/Los_Angeles")
			.put("metric", true)
			.put("tag", "foo"));
	}

	@Test
	@Ignore
	public void testCardio() {
		run(new GoogleFitCardioTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-10-31")
			.put("timezone", "America/Los_Angeles")
			.put("tag", "bar"));
	}

	@Test
	public void testFood() {
		run(new GoogleFitFoodTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-12-13")
			.put("timezone", "America/Los_Angeles")
			.put("tag", "bar"));
	}

	@Override
	protected GoogleCredentialsManager newCredentialsManager() {
		return new GoogleCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
