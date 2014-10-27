package com.zenobase.tasks.netatmo;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class NetatmoTest extends TaskTestSupport {

	@Test
	public void test() {
		NetatmoTaskManager manager = new NetatmoTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("marker", "2014-10-27");
		settings.put("hourly", true);
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected NetatmoCredentialsManager newCredentialsManager() {
		return new NetatmoCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
