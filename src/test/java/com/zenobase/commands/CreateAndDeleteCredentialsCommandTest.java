package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.zenobase.models.Identity;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.Credentials;

public class CreateAndDeleteCredentialsCommandTest {

	private final CredentialsRepository repository = mock(CredentialsRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new CreateCredentialsCommand.Handler(repository),
		new DeleteCredentialsCommand.Handler(repository)
	);

	@Test
	public void test() {
		String type = "test";
		Identity principal = new Identity();
		Credentials task = new Credentials(type, principal);

		Command command = new CreateCredentialsCommand(principal, task);
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
