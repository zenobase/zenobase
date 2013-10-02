package com.zenobase.tasks.foursquare;

import java.util.Scanner;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManager;
import com.zenobase.tasks.TaskTestSupport;

public class FoursquareTest extends TaskTestSupport {

	@Test
	@Ignore
	public void testNew() {
		TaskManager manager = new FoursquareTaskManager(apiKey, apiSecret, callbackUrl);
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
	public void testExisting() {
		FoursquareTaskManager manager = new FoursquareTaskManager(apiKey, apiSecret, callbackUrl);
		manager.execute(new FoursquareTask(bucketId, principal, getToken(), FoursquareTaskManager.formatMarker(DateTime.now().minusDays(2))));
	}
}
