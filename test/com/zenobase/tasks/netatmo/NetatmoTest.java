package com.zenobase.tasks.netatmo;

import java.util.Scanner;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManager;
import com.zenobase.tasks.TaskTestSupport;

public class NetatmoTest extends TaskTestSupport {

	@Test
	@Ignore
	public void testNew() {
		TaskManager manager = new NetatmoTaskManager(apiKey, apiSecret, callbackUrl);
		Task task = manager.newTask(bucketId, principal, Nodes.newObject());
		System.out.println(task.getAuthorizationUrl());
		ObjectNode config = Nodes.newObject();
		Scanner scanner = new Scanner(System.in);
		System.out.print("code=");
		config.put("code", scanner.nextLine());
		task = apply(manager.authorize(task, config), task);
		System.out.println(task.toJson());
		manager.execute(task);
	}

	@Test
	public void testExisting() {
		NetatmoTaskManager manager = new NetatmoTaskManager(apiKey, apiSecret, callbackUrl);
		manager.execute(new NetatmoTask(bucketId, principal, getToken(), null));
	}
}
