package com.zenobase.tasks.strava;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class StravaTest extends TaskTestSupport {

	@Test
	public void test() {
		StravaTaskManager manager = new StravaTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("marker", "2014-02-16");
		settings.put("metric", "false");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected StravaCredentialsManager newCredentialsManager() {
		return new StravaCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
