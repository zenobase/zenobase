package com.zenobase.tasks.mapmyfitness;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class MapMyFitnessTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(new MapMyFitnessTaskManager(newCredentialsManager()), Nodes.newObject("marker", "2014-11-06T00:00:00.000-08:00"));
	}

	@Override
	protected MapMyFitnessCredentialsManager newCredentialsManager() {
		return new MapMyFitnessCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
