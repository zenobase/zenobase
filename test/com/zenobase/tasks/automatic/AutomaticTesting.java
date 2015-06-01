package com.zenobase.tasks.automatic;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class AutomaticTesting extends TaskTestingSupport {

	@Test
	public void test() {
		ObjectNode settings = Nodes.newObject()
			.put("marker", "2015-05-01T00:00:00.000-07:00")
			.put("tag", "trip")
			.put("metric", "false");
		run(new AutomaticTaskManager(newCredentialsManager()), settings);
	}

	@Override
	protected AutomaticCredentialsManager newCredentialsManager() {
		return new AutomaticCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
