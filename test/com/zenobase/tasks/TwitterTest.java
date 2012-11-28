package com.zenobase.tasks;

import java.util.Scanner;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.common.Generator;

public class TwitterTest extends TaskTestSupport {

	@Test
	@Ignore
	public void testNoToken() {
		TwitterTaskManager manager = new TwitterTaskManager(apiKey, apiSecret, callbackUrl);
		TwitterTask task = new TwitterTask(bucketId, principal);
		System.out.println(manager.getAuthorizationUrl(task));
		System.out.print("verifier=");
		Scanner scanner = new Scanner(System.in);
		manager.setToken(task, scanner.nextLine()); // ?oauth_token=xxx&oauth_verifier=xxx
		manager.execute(task);
	}

	@Test
	public void testHasToken() {
		TwitterTaskManager manager = new TwitterTaskManager(apiKey, apiSecret, callbackUrl);
		manager.execute(new TwitterTask(Generator.id(), bucketId, principal, getToken()));
	}
}
