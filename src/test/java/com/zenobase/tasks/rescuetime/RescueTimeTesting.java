package com.zenobase.tasks.rescuetime;

import org.junit.jupiter.api.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class RescueTimeTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(
			new RescueTimeProductivityTaskManager(newCredentialsManager()),
			Nodes.newObject()
				.put("timezone", "America/Los_Angeles")
				.put("marker", "2014-11-01T10:00:00.000")
				.put("kind", "overview")
		);
	}

	@Override
	protected RescueTimeCredentialsManager newCredentialsManager() {
		return new RescueTimeCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
