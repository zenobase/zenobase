package com.zenobase.tasks;

import java.util.Scanner;

import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.common.Generator;

public class FoursquareTest extends TaskTestSupport {

	@Test
	@Ignore
	public void testNoToken() {
		FoursquareTaskManager manager = new FoursquareTaskManager(apiKey, apiSecret, callbackUrl);
		FoursquareTask task = new FoursquareTask();
		System.out.println(manager.getAuthorizationUrl(task));
		System.out.print("verifier=");
		Scanner scanner = new Scanner(System.in); // ?code=xxx
		manager.setToken(task, scanner.nextLine());
		manager.execute(task);
	}

	@Test
	public void testHasToken() {
		FoursquareTaskManager manager = new FoursquareTaskManager(apiKey, apiSecret, callbackUrl);
		manager.execute(new FoursquareTask(Generator.id(), getToken(), new DateTime().minusDays(7)));
	}
}
