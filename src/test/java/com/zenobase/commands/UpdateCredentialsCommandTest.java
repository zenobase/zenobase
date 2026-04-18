package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.zenobase.models.Identity;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.Credentials;

public class UpdateCredentialsCommandTest {

	private final Identity principal = new Identity();
	private final CredentialsRepository repository = mock(CredentialsRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new UpdateCredentialsCommand.Handler(repository)
	);

	@Test
	public void test() {
		Credentials from = new Credentials("do nothing", principal);

		Credentials to = from.copy();
		to.setAuthorizationUrl("http://localhost/");

		Command command = UpdateCredentialsCommand.builder(from)
			.set(Credentials.AUTHORIZATION_URL, from.getAuthorizationUrl(), to.getAuthorizationUrl())
			.build();
		when(repository.find(from.getId())).thenReturn(from.copy());
		registry.execute(command);
		verify(repository).update(to);
		reset(repository);

		Command undo = command.reverse(principal);
		when(repository.find(from.getId())).thenReturn(to.copy());
		registry.execute(undo);
		verify(repository).update(from);
		reset(repository);

		Command redo = undo.reverse(principal);
		when(repository.find(from.getId())).thenReturn(from.copy());
		registry.execute(redo);
		verify(repository).update(to);
		reset(repository);
	}
}
