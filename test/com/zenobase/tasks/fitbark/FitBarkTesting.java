package com.zenobase.tasks.fitbark;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class FitBarkTesting extends TaskTestingSupport {

	@Test
	public void test() {
		ObjectNode settings = Nodes.newObject()
			.put("marker", "2015-01-01T00:00:00.000-08:00")
			.put("name", "Jessie")
			.put("hourly", true);
		run(new FitBarkTaskManager(newCredentialsManager()), settings);
	}

	@Override
	protected FitBarkCredentialsManager newCredentialsManager() {
		return new FitBarkCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
