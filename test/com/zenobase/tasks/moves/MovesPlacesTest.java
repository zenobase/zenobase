package com.zenobase.tasks.moves;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class MovesPlacesTest extends TaskTestSupport {

	@Test
	public void test() {
		MovesPlacesTaskManager manager = new MovesPlacesTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("marker", "2014-02-06");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected MovesCredentialsManager newCredentialsManager() {
		return new MovesCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
