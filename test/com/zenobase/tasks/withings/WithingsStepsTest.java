package com.zenobase.tasks.withings;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class WithingsStepsTest extends TaskTestSupport {

	@Test
	public void test() {
		WithingsStepsTaskManager manager = new WithingsStepsTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("tag", "weight");
		settings.put("unit", "mi");
		settings.put("timezone", "Europe/Amsterdam");
		settings.put("marker", "2013-12-01");
		Task task = manager.newTask(bucketId, principal, settings);
		manager.execute(task, getCredentials()).toJson();
	}

	@Override
	protected WithingsCredentialsManager newCredentialsManager() {
		return new WithingsCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
