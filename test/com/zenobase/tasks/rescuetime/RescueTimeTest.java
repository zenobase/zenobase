package com.zenobase.tasks.rescuetime;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class RescueTimeTest {

	private final String bucketId = Generator.id();
	private final Identity principal = new Identity();
	private final String apiKey = System.getProperty("rescuetime.apiKey");

	@Before
	public void setUp() {
		Assume.assumeNotNull(apiKey);
	}

	@Test
	public void test() {
		RescueTimeProductivityTaskManager manager = new RescueTimeProductivityTaskManager();
		ObjectNode settings = Nodes.newObject();
		settings.put("key", apiKey);
		settings.put("timezone", "America/Los_Angeles");
		// settings.put("marker", "2014-02-01T10:00:00.000");
		Task task = manager.newTask(bucketId, principal, settings);
		Command command = manager.execute(task);
		System.out.println(Nodes.toString(command.toJson()));
	}
}
