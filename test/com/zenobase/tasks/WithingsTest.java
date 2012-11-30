package com.zenobase.tasks;

import java.util.Scanner;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;

public class WithingsTest extends TaskTestSupport {

	@Test
	@Ignore
	public void testNew() {
		TaskManager manager = new WithingsTaskManager(apiKey, apiSecret, callbackUrl);
		Task task = manager.newTask(bucketId, principal);
		System.out.println(manager.getAuthorizationUrl(task));
		ObjectNode config = Nodes.newObject();
		Scanner scanner = new Scanner(System.in);
		System.out.print("oauth_token=");
		config.put("oauth_token", scanner.nextLine());
		System.out.print("oauth_verifier=");
		config.put("oauth_verifier", scanner.nextLine());
		System.out.print("userid=");
		config.put("userid", scanner.nextLine());
		task = apply(manager.authorize(task, config), task);
		manager.execute(task);
	}

	@Test
	public void testExisting() {
		TaskManager manager = new WithingsTaskManager(apiKey, apiSecret, callbackUrl);
		manager.execute(new WithingsTask(bucketId, principal, getToken(), 1317928, "1353555281"));
	}
}
