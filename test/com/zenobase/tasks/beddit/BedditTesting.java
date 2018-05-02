package com.zenobase.tasks.beddit;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class BedditTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(new BedditTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("tag", "Sleep")
			.put("marker", "2015-01-01")
		);
	}

	@Override
	protected BedditCredentialsManager newCredentialsManager() {
		return new BedditCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
