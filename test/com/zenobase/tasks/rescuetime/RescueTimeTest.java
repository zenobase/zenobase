package com.zenobase.tasks.rescuetime;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class RescueTimeTest extends TaskTestSupport {

	private final String bucketId = Generator.id();
	private final Identity principal = new Identity();

	@Test
	public void test() {
		RescueTimeProductivityTaskManager manager = new RescueTimeProductivityTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("timezone", "America/Los_Angeles");
		settings.put("marker", "2014-04-01T10:00:00.000");
		settings.put("kind", "overview");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected RescueTimeCredentialsManager newCredentialsManager() {
		return new RescueTimeCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
