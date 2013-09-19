package com.zenobase.tasks.bodymedia;

import java.util.Scanner;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManager;
import com.zenobase.tasks.TaskTestSupport;

public class BodyMediaBurnTest extends TaskTestSupport {

	@Test
	@Ignore
	public void testNew() {
		TaskManager manager = new BodyMediaTaskManager(apiKey, apiSecret, callbackUrl);
		Task task = manager.newTask(bucketId, principal, Nodes.newObject());
		System.out.println(task.getAuthorizationUrl());
		ObjectNode config = Nodes.newObject();
		Scanner scanner = new Scanner(System.in);
		System.out.print("oauth_token=");
		config.put("oauth_token", scanner.nextLine());
		config.put("oauth_verifier", "");
		scanner.close();
		task = apply(manager.authorize(task, config), task);
		manager.execute(task);
	}

	@Test
	public void testExisting() {
		TaskManager manager = new BodyMediaBurnTaskManager(apiKey, apiSecret, callbackUrl);
		manager.execute(new BodyMediaBurnTask(bucketId, principal, getToken(), "2013-09-17"));
	}
}
