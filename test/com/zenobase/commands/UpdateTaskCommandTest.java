package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.joda.time.DateTime;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.services.TaskRepository;
import com.zenobase.tasks.Task;

public class UpdateTaskCommandTest {

	private final Identity principal = new Identity();
	private final TaskRepository tasks = mock(TaskRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new UpdateTaskCommand.Handler(tasks));

	@Test
	public void test() {

		Task from = new Task("do nothing", Generator.id(), principal);
		from.setCompleted(DateTime.now().minusDays(1));

		Task to = from.copy();
		to.setCompleted(DateTime.now());

		Command command = UpdateTaskCommand.builder(from)
			.set(Task.COMPLETED, from.getCompleted(), to.getCompleted())
			.build();
		when(tasks.find(from.getId())).thenReturn(from.copy());
		registry.execute(command);
		verify(tasks).update(to);
		reset(tasks);

		Command undo = command.reverse(principal);
		when(tasks.find(from.getId())).thenReturn(to.copy());
		registry.execute(undo);
		verify(tasks).update(from);
		reset(tasks);

		Command redo = undo.reverse(principal);
		when(tasks.find(from.getId())).thenReturn(from.copy());
		registry.execute(redo);
		verify(tasks).update(to);
		reset(tasks);
	}
}
