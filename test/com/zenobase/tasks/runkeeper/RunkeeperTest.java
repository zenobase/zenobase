package com.zenobase.tasks.runkeeper;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.oauth.CustomX509TrustManager;
import com.zenobase.tasks.TaskTestSupport;

public class RunkeeperTest extends TaskTestSupport {

	@BeforeClass
	public static void setUpSSL() {
		CustomX509TrustManager.setDefault();
	}

	@Test
	public void testActivities() {
		run(new RunkeeperActivitiesTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-11-13")
			.put("unit", "mi")
			.put("timezone", "America/Los_Angeles"));
	}

	@Test
	@Ignore
	public void testWeight() {
		run(new RunkeeperWeightTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-11-01")
			.put("tag", "Me")
			.put("unit", "lb")
			.put("timezone", "America/Los_Angeles"));
	}

	@Override
	protected RunkeeperCredentialsManager newCredentialsManager() {
		return new RunkeeperCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
