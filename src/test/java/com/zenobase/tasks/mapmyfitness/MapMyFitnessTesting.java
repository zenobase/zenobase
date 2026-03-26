package com.zenobase.tasks.mapmyfitness;

import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class MapMyFitnessTesting extends TaskTestingSupport {

	@Test
	@Ignore
	public void testActivities() {
		run(
				new MapMyFitnessActivitiesTaskManager(newCredentialsManager()),
				Nodes.newObject("marker", "2014-11-06T00:00:00.000-08:00"));
	}

	@Test
	@Ignore
	public void testWeight() {
		run(
				new MapMyFitnessWeightTaskManager(newCredentialsManager()),
				Nodes.newObject("marker", "2015-01-01T00:00:00.000-08:00"));
	}

	@Test
	public void testSleep() {
		run(
				new MapMyFitnessSleepTaskManager(newCredentialsManager()),
				Nodes.newObject("marker", "2015-01-01T00:00:00.000-08:00"));
	}

	@Override
	protected MapMyFitnessCredentialsManager newCredentialsManager() {
		return new MapMyFitnessCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
