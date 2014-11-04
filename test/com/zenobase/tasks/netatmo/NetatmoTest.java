package com.zenobase.tasks.netatmo;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestSupport;

public class NetatmoTest extends TaskTestSupport {

	@Test
	public void test() {
		run(new NetatmoTaskManager(newCredentialsManager()), Nodes.newObject("marker", "2014-10-27").put("hourly", true));
	}

	@Override
	protected NetatmoCredentialsManager newCredentialsManager() {
		return new NetatmoCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
