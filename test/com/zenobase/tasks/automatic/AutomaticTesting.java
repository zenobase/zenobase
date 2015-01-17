package com.zenobase.tasks.automatic;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class AutomaticTesting extends TaskTestingSupport {

	@Test
	public void test() {
		ObjectNode settings = Nodes.newObject()
			.put("marker", "2015-01-01T12:00:00.000-08:00")
			.put("tag", "Trip")
			.put("metric", "false");
		run(new AutomaticTaskManager(newCredentialsManager()), settings);
	}

	@Override
	protected AutomaticCredentialsManager newCredentialsManager() {
		return new AutomaticCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
