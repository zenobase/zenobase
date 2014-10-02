package com.zenobase.tasks.jawbone;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class JawboneSleepTest extends TaskTestSupport {

	@Test
	public void test() {
		JawboneSleepTaskManager manager = new JawboneSleepTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("tag", "sleep");
		settings.put("marker", "2014-01-01");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected JawboneCredentialsManager newCredentialsManager() {
		return new JawboneCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
