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
	private final TaskRepository repository = mock(TaskRepository.class);
	private final CommandHandlerRegistry registry =
			CommandHandlerRegistry.containing(new UpdateTaskCommand.Handler(repository));

	@Test
	public void test() {

		Task from = new Task("do nothing", Generator.id(), principal);
		from.setCompleted(DateTime.now().minusDays(1));

		Task to = from.copy();
		to.setCompleted(DateTime.now());

		Command command = UpdateTaskCommand.builder(from)
				.set(Task.COMPLETED, from.getCompleted(), to.getCompleted())
				.build();
		when(repository.find(from.getId())).thenReturn(from.copy());
		registry.execute(command);
		verify(repository).update(to, command.getTimestamp());
		reset(repository);

		Command undo = command.reverse(principal);
		when(repository.find(from.getId())).thenReturn(to.copy());
		registry.execute(undo);
		verify(repository).update(from, undo.getTimestamp());
		reset(repository);

		Command redo = undo.reverse(principal);
		when(repository.find(from.getId())).thenReturn(from.copy());
		registry.execute(redo);
		verify(repository).update(to, redo.getTimestamp());
		reset(repository);
	}
}
