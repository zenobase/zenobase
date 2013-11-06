package com.zenobase.tasks.fitbit;

import org.junit.Ignore;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class FitbitTest extends TaskTestSupport {

	@Test
	@Ignore
	public void test() {
		FitbitTaskManager manager = new FitbitTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("tag", "walk");
		settings.put("marker", "2013-11-01");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Test
	@Ignore
	public void testIntraday() {
		FitbitIntradayTaskManager manager = new FitbitIntradayTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("marker", "2013-11-01");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected FitbitCredentialsManager newCredentialsManager() {
		return new FitbitCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
