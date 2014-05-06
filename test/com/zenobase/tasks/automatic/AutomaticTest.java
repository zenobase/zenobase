package com.zenobase.tasks.automatic;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class AutomaticTest extends TaskTestSupport {

	@Test
	public void test() {
		AutomaticTaskManager manager = new AutomaticTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("marker", "2014-02-16");
		settings.put("metric", "false");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected AutomaticCredentialsManager newCredentialsManager() {
		return new AutomaticCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
