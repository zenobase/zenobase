package com.zenobase.commands;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class SuspendUserCommandTest {

	private final UserRepository users = mock(UserRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new SuspendUserCommand.Handler(users));

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
}
