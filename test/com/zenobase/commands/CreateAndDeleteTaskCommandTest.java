package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.services.TaskRepository;
import com.zenobase.tasks.Task;

public class CreateAndDeleteTaskCommandTest {

	private final TaskRepository tasks = mock(TaskRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new CreateTaskCommand.Handler(tasks),
		new DeleteTaskCommand.Handler(tasks));

	@Test
	public void test() {

		Identity principal = new Identity();
		String bucketId = Generator.id();
		String type = "test";
		Task task = new Task(type, bucketId, principal);

		Command command = new CreateTaskCommand(principal, task);
		registry.execute(command);
		verify(tasks).store(task, command.getTimestamp());
		reset(tasks);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(tasks).delete(task.getId());
		reset(tasks);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(tasks).store(task, redo.getTimestamp());
		reset(tasks);
	}
}
