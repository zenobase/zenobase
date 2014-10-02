package com.zenobase.tasks.jawbone;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class JawboneStepsTest extends TaskTestSupport {

	@Test
	public void test() {
		JawboneStepsTaskManager manager = new JawboneStepsTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("tag", "steps");
		settings.put("marker", "2014-01-01");
		settings.put("hourly", false);
		settings.put("metric", true);
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected JawboneCredentialsManager newCredentialsManager() {
		return new JawboneCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
