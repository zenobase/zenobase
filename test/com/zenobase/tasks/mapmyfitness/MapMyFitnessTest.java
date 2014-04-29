package com.zenobase.tasks.mapmyfitness;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class MapMyFitnessTest extends TaskTestSupport {

	@Test
	public void test() {
		MapMyFitnessTaskManager manager = new MapMyFitnessTaskManager(newCredentialsManager());
		ObjectNode settings = Nodes.newObject();
		settings.put("marker", "2014-04-17T15:09:58.000-07:00");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected MapMyFitnessCredentialsManager newCredentialsManager() {
		return new MapMyFitnessCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
