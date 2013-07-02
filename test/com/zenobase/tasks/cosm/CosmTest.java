package com.zenobase.tasks.cosm;

import java.util.Scanner;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManager;
import com.zenobase.tasks.TaskTestSupport;

public class CosmTest extends TaskTestSupport {

	@Test
	@Ignore
	public void testNew() {
		TaskManager manager = new CosmTaskManager(apiKey, apiSecret, callbackUrl);
		Task task = manager.newTask(bucketId, principal, Nodes.newObject());
		System.out.println(task.getAuthorizationUrl());
		ObjectNode config = Nodes.newObject();
		Scanner scanner = new Scanner(System.in);
		System.out.print("code=");
		config.put("code", scanner.nextLine());
		scanner.close();
		task = apply(manager.authorize(task, config), task);
		manager.execute(task);
	}

	@Test
	@Ignore
	public void testExisting() {
		CosmTaskManager manager = new CosmTaskManager(apiKey, apiSecret, callbackUrl);
		manager.execute(new CosmTask(bucketId, principal, 127524, getToken(), null));
	}
}
