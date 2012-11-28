package com.zenobase.tasks;

import java.util.Scanner;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;

public class TwitterTest extends TaskTestSupport {

	@Test
	@Ignore
	public void testNew() {
		TaskManager manager = new TwitterTaskManager(apiKey, apiSecret, callbackUrl);
		Task task = manager.newTask(bucketId, principal);
		System.out.println(manager.getConfigureUrl(task));
		ObjectNode config = Nodes.newObject();
		Scanner scanner = new Scanner(System.in);
		System.out.print("oauth_token=");
		config.put("oauth_token", scanner.nextLine());
		System.out.print("oauth_verifier=");
		config.put("oauth_verifier", scanner.nextLine());
		task = getTo(manager.configure(task, config));
		manager.execute(task);
	}

	@Test
	public void testExisting() {
		TaskManager manager = new TwitterTaskManager(apiKey, apiSecret, callbackUrl);
		manager.execute(new TwitterTask(Generator.id(), Task.State.READY, bucketId, principal, getToken()));
	}
}
