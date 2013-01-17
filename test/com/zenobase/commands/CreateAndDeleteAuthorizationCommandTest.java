package com.zenobase.commands;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationRepository;

public class CreateAndDeleteAuthorizationCommandTest {

	private final AuthorizationRepository authorizations = mock(AuthorizationRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new CreateAuthorizationCommand.Handler(authorizations),
		new DeleteAuthorizationCommand.Handler(authorizations));

	@Test
	public void test() {

		Identity principal = new Identity();
		Authorization authorization = new Authorization(principal, new Identity(), Generator.id());

		Command command = new CreateAuthorizationCommand(principal, authorization);
		registry.execute(command);
		verify(authorizations).store(authorization, command.getTimestamp());
		reset(authorizations);

		Command undo = command.reverse(principal);
		registry.execute(undo);
		verify(authorizations).delete(authorization.getId());
		reset(authorizations);

		Command redo = undo.reverse(principal);
		registry.execute(redo);
		verify(authorizations).store(authorization, redo.getTimestamp());
		reset(authorizations);
	}
}
