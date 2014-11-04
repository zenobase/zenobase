package com.zenobase.tasks.sleepcloud;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestSupport;
import com.zenobase.tasks.google.GoogleCredentialsManager;

public class SleepCloudTest extends TaskTestSupport {

	@Test
	public void test() {
		run(new SleepCloudTaskManager(newCredentialsManager()), Nodes.newObject("tag", "Sleep"));
	}

	@Override
	protected GoogleCredentialsManager newCredentialsManager() {
		return new GoogleCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
