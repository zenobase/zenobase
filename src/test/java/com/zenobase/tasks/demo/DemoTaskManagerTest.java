package com.zenobase.tasks.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.commands.Command;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.Task.Status;
import com.zenobase.tasks.TaskManagerTestSupport;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DemoTaskManagerTest extends TaskManagerTestSupport {

	@Test
	public void test() {
		var manager = new DemoTaskManager(Duration.ZERO);
		Identity principal = new Identity();
		Task task = manager.newTask(Generator.id(), principal, Nodes.newObject());

		Command commands = manager.execute(task);
		task = apply(commands, task);

		List<CreateEventCommand> creates = commands(commands, CreateEventCommand.class);
		assertThat(creates).hasSize(1);
		Event event = creates.getFirst().getEvent();
		assertThat(event.getValues(Event.TAG)).containsExactly("test");
		assertThat(event.getValue(Event.AUTHOR)).isEqualTo(principal);
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.getUndoId()).isEqualTo(commands.getId());
	}
}
