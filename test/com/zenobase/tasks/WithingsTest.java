package com.zenobase.tasks;

import java.util.Scanner;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.common.Generator;

public class WithingsTest extends TaskTestSupport {

	@Test
	@Ignore
	public void testNoToken() {
		WithingsTaskManager manager = new WithingsTaskManager(apiKey, apiSecret, callbackUrl);
		WithingsTask task = new WithingsTask();
		System.out.println(manager.getAuthorizationUrl(task));
		System.out.print("verifier=");
		Scanner scanner = new Scanner(System.in);
		manager.setToken(task, scanner.nextLine()); // ?userid=xxx&oauth_token=xxx&oauth_verifier=xxx
		task.setUserId(1317928);
		manager.execute(task);
	}

	@Test
	public void testHasToken() {
		WithingsTaskManager manager = new WithingsTaskManager(apiKey, apiSecret, callbackUrl);
		manager.execute(new WithingsTask(Generator.id(), getToken(), 1317928, "1353555281"));
	}
}
