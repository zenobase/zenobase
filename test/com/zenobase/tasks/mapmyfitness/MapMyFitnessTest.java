package com.zenobase.tasks.mapmyfitness;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestSupport;

public class MapMyFitnessTest extends TaskTestSupport {

	@Test
	public void test() {
		run(new MapMyFitnessTaskManager(newCredentialsManager()), Nodes.newObject("marker", "2014-04-17T15:09:58.000-07:00"));
	}

	@Override
	protected MapMyFitnessCredentialsManager newCredentialsManager() {
		return new MapMyFitnessCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
