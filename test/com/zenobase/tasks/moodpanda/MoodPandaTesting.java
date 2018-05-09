package com.zenobase.tasks.moodpanda;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import play.test.Helpers;

import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MoodPandaTesting {

	private final String apiKey = System.getProperty("moodpanda.apiKey");

	@Before
	public void setUp() {
		Assume.assumeNotNull(apiKey);
	}

	@Test
	public void test() {
		Helpers.running(Helpers.fakeApplication(), () -> {
			ObjectNode settings = Nodes.newObject()
				.put("tag", "moo")
				.put("email", "eric.jain@gmail.com")
				.put("marker", "2014-04-01T00:00:00-08:00");
			MoodPandaTaskManager manager = new MoodPandaTaskManager(apiKey);
			Task task = manager.newTask(Generator.id(), new Identity(), settings);
			System.err.println(Nodes.toString(manager.execute(task).toJson()));
		});
	}
}
