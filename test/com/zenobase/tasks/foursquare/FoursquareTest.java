package com.zenobase.tasks.foursquare;

import org.junit.Ignore;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class FoursquareTest extends TaskTestSupport {

	@Test
	@Ignore
	public void test() {
		OAuthTaskManager manager = new FoursquareTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("marker", "2013-11-01");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected FoursquareCredentialsManager newCredentialsManager() {
		return new FoursquareCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
