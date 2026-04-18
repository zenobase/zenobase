package com.zenobase.tasks.netatmo;

import org.junit.jupiter.api.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class NetatmoTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(
			new NetatmoTaskManager(newCredentialsManager()),
			Nodes.newObject().put("marker", "2019-01-01").put("modules", true).put("hourly", true)
		);
	}

	@Override
	protected NetatmoCredentialsManager newCredentialsManager() {
		return new NetatmoCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
