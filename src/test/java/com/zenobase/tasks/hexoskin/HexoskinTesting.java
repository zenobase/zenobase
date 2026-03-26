package com.zenobase.tasks.hexoskin;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class HexoskinTesting extends TaskTestingSupport {

	@Test
	public void testActivities() {
		run(
				new HexoskinActivitiesTaskManager(newCredentialsManager()),
				Nodes.newObject()
						.put("marker", "2017-06-01")
						.put("timezone", "America/Los_Angeles")
						.put("tag", "Training"));
	}

	@Test
	@Ignore
	public void testSleep() {
		run(
				new HexoskinSleepTaskManager(newCredentialsManager()),
				Nodes.newObject()
						.put("marker", "2010-07-01")
						.put("timezone", "America/Los_Angeles")
						.put("tag", "Sleep"));
	}

	@Override
	protected HexoskinCredentialsManager newCredentialsManager() {
		return new HexoskinCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
