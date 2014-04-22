package com.zenobase.tasks.sleepcloud;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;
import com.zenobase.tasks.google.GoogleCredentialsManager;

public class SleepCloudTest extends TaskTestSupport {

	@Test
	public void test() {
		OAuthTaskManager manager = new SleepCloudTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject("tag", "Sleep");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected GoogleCredentialsManager newCredentialsManager() {
		return new GoogleCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
