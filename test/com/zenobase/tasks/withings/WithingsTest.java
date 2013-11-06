package com.zenobase.tasks.withings;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class WithingsTest extends TaskTestSupport {

	@Test
	public void test() {
		WithingsTaskManager manager = new WithingsTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("tag", "weight");
		settings.put("unit", "lb");
		settings.put("timezone", "America/Los_Angeles");
		settings.put("marker", "2013-11-01");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected WithingsCredentialsManager newCredentialsManager() {
		return new WithingsCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
