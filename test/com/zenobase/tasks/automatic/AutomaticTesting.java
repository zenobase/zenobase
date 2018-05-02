package com.zenobase.tasks.automatic;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class AutomaticTesting extends TaskTestingSupport {

	@Test
	public void test() {
		ObjectNode settings = Nodes.newObject()
			.put("marker", "2016-09-15T00:00:00.000-07:00")
			.put("tag", "trip")
			.put("metric", "false");
		run(new AutomaticTaskManager(newCredentialsManager()), settings);
	}

	@Override
	protected AutomaticCredentialsManager newCredentialsManager() {
		return new AutomaticCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
