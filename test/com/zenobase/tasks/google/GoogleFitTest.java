package com.zenobase.tasks.google;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class GoogleFitTest extends TaskTestSupport {

	public void testActivities() {
		OAuthTaskManager manager = new GoogleFitActivitiesTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject()
			.put("marker", "2014-10-31")
			.put("timezone", "America/Los_Angeles")
			.put("metric", true)
			.put("derived", true);
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	public void testWeight() {
		OAuthTaskManager manager = new GoogleFitWeightTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject()
			.put("marker", "2014-10-31")
			.put("timezone", "America/Los_Angeles")
			.put("metric", true)
			.put("tag", "foo");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Test
	public void testCardio() {
		OAuthTaskManager manager = new GoogleFitCardioTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject()
			.put("marker", "2014-10-31")
			.put("timezone", "America/Los_Angeles")
			.put("tag", "bar");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected GoogleCredentialsManager newCredentialsManager() {
		return new GoogleCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
