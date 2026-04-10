package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationRepository;

public class CreateAndDeleteAuthorizationCommandTest {

	private final AuthorizationRepository repository = mock(AuthorizationRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
			new CreateAuthorizationCommand.Handler(repository), new DeleteAuthorizationCommand.Handler(repository));

	@Test
	public void test() {

		Identity principal = new Identity();
		Authorization authorization = new Authorization(principal, new Identity(), Generator.id());

		Command command = new CreateAuthorizationCommand(principal, authorization);
		registry.execute(command);
		verify(repository).store(authorization);
		reset(repository);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(repository).delete(authorization.getId());
		reset(repository);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(repository).store(authorization);
		reset(repository);
	}
}
