package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.repositories.TaskRepository;
import com.zenobase.tasks.Task;

public class CreateAndDeleteTaskCommandTest {

	private final TaskRepository repository = mock(TaskRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new CreateTaskCommand.Handler(repository),
		new DeleteTaskCommand.Handler(repository)
	);

	@Test
	public void test() {
		Identity principal = new Identity();
		String bucketId = Generator.id();
		String type = "test";
		Task task = new Task(type, bucketId, principal);

		Command command = new CreateTaskCommand(principal, task);
		registry.execute(command);
		verify(repository).store(task);
		reset(repository);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(repository).delete(task.getId());
		reset(repository);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(repository).store(task);
		reset(repository);
	}
}
