package com.zenobase.tasks.runkeeper;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class RunkeeperTest extends TaskTestSupport {

	@Test
	public void test() {
		RunkeeperTaskManager manager = new RunkeeperTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("marker", "2013-11-01");
		settings.put("unit", "mi");
		settings.put("timezone", "America/Los_Angeles");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected RunkeeperCredentialsManager newCredentialsManager() {
		return new RunkeeperCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
