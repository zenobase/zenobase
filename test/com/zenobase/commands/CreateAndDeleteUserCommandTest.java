package com.zenobase.commands;

import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class CreateAndDeleteUserCommandTest {

	private final UserRepository users = mock(UserRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new CreateUserCommand.Handler(users),
		new DeleteUserCommand.Handler(users));

	@Test
	public void test() {

		User user = new User(Generator.id(), "tester");

		Command command = new CreateUserCommand(user.asIdentity(), user);
		registry.execute(command);
		verify(users).store(user);
		reset(users);

		Command undo = command.reverse(user.asIdentity());
		registry.execute(undo);
		verify(users).delete(user);
		reset(users);

		Command redo = undo.reverse(user.asIdentity());
		registry.execute(redo);
		verify(users).store(user);
		reset(users);
	}
}
