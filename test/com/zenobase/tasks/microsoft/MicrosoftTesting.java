package com.zenobase.tasks.microsoft;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

import org.junit.Ignore;
import org.junit.Test;

public class MicrosoftTesting extends TaskTestingSupport {

	@Test
	@Ignore
	public void testActivities() {
		run(new MicrosoftHealthActivitiesTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-01-01T00:00:00Z")
			.put("timezone", "Europe/Berlin")
			.put("metric", true));
	}

	@Test
	public void testSteps() {
		run(new MicrosoftHealthStepsTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-01-01T00:00:00Z")
			.put("timezone", "Europe/Berlin")
			.put("tag", "Summary")
			.put("hourly", false)
			.put("metric", false));
	}

	@Override
	protected MicrosoftHealthCredentialsManager newCredentialsManager() {
		return new MicrosoftHealthCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
