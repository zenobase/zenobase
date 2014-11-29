package com.zenobase.tasks.bodymedia;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class BodyMediaTesting extends TaskTestingSupport {

	@Test
	public void testBurn() {
		run(new BodyMediaBurnTaskManager(newCredentialsManager()), Nodes.newObject("marker", "2013-11-01"));
	}

	@Test
	@Ignore
	public void testSteps() {
		run(new BodyMediaStepsTaskManager(newCredentialsManager()), Nodes.newObject("marker", "2013-11-11"));
	}

	@Test
	@Ignore
	public void testSleep() {
		run(new BodyMediaSleepTaskManager(newCredentialsManager()), Nodes.newObject("marker", "2012-11-01"));
	}

	@Override
	protected BodyMediaCredentialsManager newCredentialsManager() {
		return new BodyMediaCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
