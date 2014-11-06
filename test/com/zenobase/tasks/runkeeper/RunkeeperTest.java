package com.zenobase.tasks.runkeeper;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.oauth.CustomX509TrustManager;
import com.zenobase.tasks.TaskTestSupport;

public class RunkeeperTest extends TaskTestSupport {

	@Test
	public void test() {
		CustomX509TrustManager.setDefault();
		run(new RunkeeperTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-11-06")
			.put("unit", "mi")
			.put("timezone", "America/Los_Angeles"));
	}

	@Override
	protected RunkeeperCredentialsManager newCredentialsManager() {
		return new RunkeeperCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
