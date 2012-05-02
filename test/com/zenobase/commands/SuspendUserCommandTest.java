package com.zenobase.commands;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.services.UserManager;

public class SuspendUserCommandTest {

	private final UserManager users = mock(UserManager.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.create(
		new SuspendUserCommand.Handler(users));

	@Test
	public void test() {

		User user = new User(Generator.id(), "tester");
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
