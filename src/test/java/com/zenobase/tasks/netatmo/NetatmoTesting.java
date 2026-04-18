package com.zenobase.tasks.netatmo;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;
import org.junit.jupiter.api.Test;

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
