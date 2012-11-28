package com.zenobase.tasks;

import java.util.Scanner;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.common.Generator;

public class FitbitTest extends TaskTestSupport {

	@Test
	@Ignore
	public void testNoToken() {
		FitbitTaskManager manager = new FitbitTaskManager(apiKey, apiSecret, callbackUrl);
		FitbitTask task = new FitbitTask(bucketId, principal);
		System.out.println(manager.getAuthorizationUrl(task));
		System.out.print("verifier=");
		Scanner scanner = new Scanner(System.in);
		manager.setToken(task, scanner.nextLine()); // ?oauth_token=xxx&oauth_verifier=xxx
		manager.execute(task);
	}

	@Test
	public void testHasToken() {
		FitbitTaskManager manager = new FitbitTaskManager(apiKey, apiSecret, callbackUrl);
		manager.execute(new FitbitTask(Generator.id(), bucketId, principal, getToken(), "1353555281"));
	}
}
