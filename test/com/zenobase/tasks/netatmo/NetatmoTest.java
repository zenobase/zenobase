package com.zenobase.tasks.netatmo;

import java.util.Scanner;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.scribe.model.Token;

import com.zenobase.json.Nodes;
import com.zenobase.oauth.ExpiringToken;
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
		manager.execute(task);
	}

	@Test
	public void testReauthorization() {
		Token token = getToken();
		Task task = new NetatmoTask(bucketId, principal, new ExpiringToken(token.getToken(), "", DateTime.now().minusHours(1), "5154d346197759768f000001|c5b102abf2baf45e982b9087d3847f00"), null);
		NetatmoTaskManager manager = new NetatmoTaskManager(apiKey, apiSecret, callbackUrl);
		manager.execute(task);
	}

	@Test
	@Ignore
	public void testExisting() {
		NetatmoTaskManager manager = new NetatmoTaskManager(apiKey, apiSecret, callbackUrl);
		manager.execute(new NetatmoTask(bucketId, principal, getToken(), null));
	}
}
