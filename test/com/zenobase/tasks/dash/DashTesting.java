package com.zenobase.tasks.dash;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class DashTesting extends TaskTestingSupport {

	@Test
	public void test() {
		ObjectNode settings = Nodes.newObject()
			.put("marker", "2016-03-28T15:00:00.000-07:00")
			.put("tag", "trip")
			.put("timezone", "America/Los_Angeles");
		run(new DashTaskManager(newCredentialsManager()), settings);
	}

	@Override
	protected DashCredentialsManager newCredentialsManager() {
		return new DashCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
