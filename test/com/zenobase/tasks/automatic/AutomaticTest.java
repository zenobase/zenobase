package com.zenobase.tasks.automatic;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestSupport;

public class AutomaticTest extends TaskTestSupport {

	@Test
	public void test() {
		ObjectNode settings = Nodes.newObject()
			.put("marker", "2014-04-20T20:00:00.000-07:00")
			.put("tag", "Trip")
			.put("metric", "false");
		run(new AutomaticTaskManager(newCredentialsManager()), settings);
	}

	@Override
	protected AutomaticCredentialsManager newCredentialsManager() {
		return new AutomaticCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
