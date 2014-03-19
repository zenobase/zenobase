package com.zenobase.tasks.withings;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class WithingsSleepTest extends TaskTestSupport {

	@Test
	public void test() {
		WithingsSleepTaskManager manager = new WithingsSleepTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("tag", "Sleep");
		settings.put("marker", "2014-03-01T00:00:00Z");
		settings.put("timezone", "America/Los_Angeles");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected WithingsCredentialsManager newCredentialsManager() {
		return new WithingsCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
