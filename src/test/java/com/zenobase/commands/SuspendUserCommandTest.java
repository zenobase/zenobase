package com.zenobase.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.zenobase.models.User;
import com.zenobase.repositories.UserRepository;
import org.junit.jupiter.api.Test;

public class SuspendUserCommandTest {

	private final UserRepository users = mock(UserRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new SuspendUserCommand.Handler(users)
	);

	@Test
	public void test() {
		User user = new User("tester");
		when(users.find(user.getName())).thenReturn(user);

		Command command = new SuspendUserCommand(user.asIdentity(), user.getName(), true);
		registry.execute(command);
		assertThat(user.isSuspended()).as("user is suspended").isTrue();

		Command undo = command.reverse(user.asIdentity());
		registry.execute(undo);
		assertThat(user.isSuspended()).as("user is suspended").isFalse();

		Command redo = undo.reverse(user.asIdentity());
		registry.execute(redo);
		assertThat(user.isSuspended()).as("user is suspended").isTrue();
	}

	@Test
	public void testSuspendNonExistentUser() {
		User user = new User("tester");

		Command command = new SuspendUserCommand(user.asIdentity(), user.getName(), true);
		assertThatThrownBy(() -> registry.execute(command)).isInstanceOf(NonExistentUserException.class);
	}
}
