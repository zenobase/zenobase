package com.zenobase.tasks;

import java.util.Scanner;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;

public class FoursquareTest extends TaskTestSupport {

	@Test
	@Ignore
	public void testNew() {
		TaskManager manager = new FoursquareTaskManager(apiKey, apiSecret, callbackUrl);
		Task task = manager.newTask(bucketId, principal);
		System.out.println(manager.getAuthorizationUrl(task));
		ObjectNode config = Nodes.newObject();
		Scanner scanner = new Scanner(System.in);
		System.out.print("code=");
		config.put("code", scanner.nextLine());
		task = apply(manager.authorize(task, config), task);
		manager.execute(task);
	}

	@Test
	public void testExisting() {
		FoursquareTaskManager manager = new FoursquareTaskManager(apiKey, apiSecret, callbackUrl);
		manager.execute(new FoursquareTask(bucketId, principal, getToken(), FoursquareTaskManager.formatMarker(DateTime.now().minusDays(1))));
	}
}
