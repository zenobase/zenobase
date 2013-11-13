package com.zenobase.tasks.bodymedia;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class BodyMediaSummaryTest extends TaskTestSupport {

	@Test
	public void test() {
		BodyMediaSummaryTaskManager manager = new BodyMediaSummaryTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("marker", "2013-11-01");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected BodyMediaCredentialsManager newCredentialsManager() {
		return new BodyMediaCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
