package com.zenobase.tasks.wakatime;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class WakaTimeTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(new WakaTimeTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2015-04-01")
			.put("tag", "project"));
	}

	@Override
	protected WakaTimeCredentialsManager newCredentialsManager() {
		return new WakaTimeCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
