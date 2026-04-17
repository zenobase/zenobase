package com.zenobase.commands;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.zenobase.models.User;
import com.zenobase.repositories.UserRepository;

public class CreateAndDeleteUserCommandTest {

	private final UserRepository repository = mock(UserRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
			new CreateUserCommand.Handler(repository), new DeleteUserCommand.Handler(repository));

	@Test
	public void test() {

		User user = new User("tester");

		Command command = new CreateUserCommand(user.asIdentity(), user);
		registry.execute(command);
		verify(repository).store(user);
		reset(repository);

		Command undo = command.reverse(user.asIdentity());
		when(repository.delete(user)).thenReturn(true);
		registry.execute(undo);
		verify(repository).delete(user);
		reset(repository);

		Command redo = undo.reverse(user.asIdentity());
		registry.execute(redo);
		verify(repository).store(user);
		reset(repository);
	}

	@Test
	public void testDeleteNonExistentUser() {

		User user = new User("tester");

		Command command = new DeleteUserCommand(user.asIdentity(), user);
		assertThatThrownBy(() -> registry.execute(command)).isInstanceOf(NonExistentUserException.class);
	}
}
