package com.zenobase.tasks.rescuetime;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestSupport;

public class RescueTimeTest extends TaskTestSupport {

	@Test
	public void test() {
		run(new RescueTimeProductivityTaskManager(newCredentialsManager()), Nodes.newObject()
		.put("timezone", "America/Los_Angeles")
		.put("marker", "2014-04-01T10:00:00.000")
		.put("kind", "overview"));
	}

	@Override
	protected RescueTimeCredentialsManager newCredentialsManager() {
		return new RescueTimeCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
