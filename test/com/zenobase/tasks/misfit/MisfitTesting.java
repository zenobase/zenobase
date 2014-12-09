package com.zenobase.tasks.misfit;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class MisfitTesting extends TaskTestingSupport {

	@Test
	@Ignore
	public void testActvities() {
		run(new MisfitActivitiesTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-10-01T00:00:00-08:00"));
	}

	@Test
	@Ignore
	public void testSteps() {
		run(new MisfitStepsTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-11-01T00:00:00-08:00")
			.put("tag", "Steps")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	public void testSleep() {
		run(new MisfitSleepTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-11-01T00:00:00-08:00")
			.put("tag", "Sleep"));
	}

	@Override
	protected MisfitCredentialsManager newCredentialsManager() {
		return new MisfitCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
