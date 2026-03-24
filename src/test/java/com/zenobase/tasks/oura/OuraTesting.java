package com.zenobase.tasks.oura;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class OuraTesting extends TaskTestingSupport {

	@Test
	@Ignore
	public void testSteps() {
		run(new OuraStepsTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2018-12-01T00:00:00+02:00"));
	}

	@Test
	@Ignore
	public void testSleep() {
		run(new OuraSleepTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2018-12-01T00:00:00+02:00"));
	}

	@Test
	public void testReadiness() {
		run(new OuraReadinessTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("timezone", "Europe/Berlin")
			.put("marker", "2018-12-01T00:00:00+02:00"));
	}

	@Override
	protected OuraCredentialsManager newCredentialsManager() {
		return new OuraCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
