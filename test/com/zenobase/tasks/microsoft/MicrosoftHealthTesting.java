package com.zenobase.tasks.microsoft;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

import org.junit.Ignore;
import org.junit.Test;

public class MicrosoftHealthTesting extends TaskTestingSupport {

	@Test
	@Ignore
	public void testActivities() {
		run(new MicrosoftHealthActivitiesTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2015-07-01")
			.put("timezone", "Europe/Berlin")
			.put("metric", true));
	}

	@Test
	public void testSteps() {
		run(new MicrosoftHealthStepsTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2015-07-24")
			.put("timezone", "Europe/Berlin")
			.put("tag", "Summary")
			.put("hourly", true)
			.put("metric", true));
	}

	@Test
	@Ignore
	public void testSleep() {
		run(new MicrosoftHealthSleepTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2015-01-01")
			.put("timezone", "Europe/Berlin")
			.put("tag", "zzz"));
	}

	@Override
	protected MicrosoftHealthCredentialsManager newCredentialsManager() {
		return new MicrosoftHealthCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
