package com.zenobase.tasks;

import java.util.Scanner;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.LocalDate;
import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;

public class FitbitTest extends TaskTestSupport {

	@Test
	@Ignore
	public void testNew() {
		TaskManager manager = new FitbitTaskManager(apiKey, apiSecret, callbackUrl);
		Task task = manager.newTask(bucketId, principal);
		System.out.println(manager.getAuthorizationUrl(task));
		ObjectNode config = Nodes.newObject();
		Scanner scanner = new Scanner(System.in);
		System.out.print("oauth_token=");
		config.put("oauth_token", scanner.nextLine());
		System.out.print("oauth_verifier=");
		config.put("oauth_verifier", scanner.nextLine());
		task = apply(manager.authorize(task, config), task);
		manager.execute(task);
	}

	@Test
	public void testExisting() {
		TaskManager manager = new FitbitTaskManager(apiKey, apiSecret, callbackUrl);
		manager.execute(new FitbitTask(bucketId, principal, getToken(), LocalDate.now().minusDays(3).toString()));
	}
}
