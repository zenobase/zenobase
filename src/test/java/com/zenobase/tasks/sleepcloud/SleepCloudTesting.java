package com.zenobase.tasks.sleepcloud;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;
import com.zenobase.tasks.google.GoogleCredentialsManager;
import org.junit.jupiter.api.Test;

public class SleepCloudTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(new SleepCloudTaskManager(newCredentialsManager()), Nodes.newObject("tag", "Sleep"));
	}

	@Override
	protected GoogleCredentialsManager newCredentialsManager() {
		return new GoogleCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
