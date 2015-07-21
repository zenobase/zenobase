package com.zenobase.tasks.microsoft;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

import org.junit.Test;

public class MicrosoftTesting extends TaskTestingSupport {

	@Test
	public void testActivities() {
		run(new MicrosoftHealthActivitiesTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-01-01T00:00:00-08:00")
			.put("metric", true));
	}

	@Override
	protected MicrosoftHealthCredentialsManager newCredentialsManager() {
		return new MicrosoftHealthCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
