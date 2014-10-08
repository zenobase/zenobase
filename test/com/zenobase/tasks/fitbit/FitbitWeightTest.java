package com.zenobase.tasks.fitbit;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class FitbitWeightTest extends TaskTestSupport {

	@Test
	public void test() {
		FitbitWeightTaskManager manager = new FitbitWeightTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("tag", "weight");
		settings.put("marker", "2014-10-01");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected FitbitCredentialsManager newCredentialsManager() {
		return new FitbitCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
