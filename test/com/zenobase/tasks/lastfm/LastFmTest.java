package com.zenobase.tasks.lastfm;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class LastFmTest extends TaskTestSupport {

	@Test
	public void test() {
		LastFmTaskManager manager = new LastFmTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("tag", "track");
		settings.put("timezone", "America/Los_Angeles");
		settings.put("marker", "2014-01-01");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected LastFmCredentialsManager newCredentialsManager() {
		return new LastFmCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
