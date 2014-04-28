package com.zenobase.tasks.reporter;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;
import com.zenobase.tasks.dropbox.DropboxCredentialsManager;

public class ReporterTest extends TaskTestSupport {

	@Test
	public void test() {
		OAuthTaskManager manager = new ReporterTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("folder", "Apps/Reporter-App");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected DropboxCredentialsManager newCredentialsManager() {
		return new DropboxCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
