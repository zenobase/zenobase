package com.zenobase.commands;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.services.UserManager;

public class ChangeUserVerifiedCommandTest {

	private final UserManager users = mock(UserManager.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new ChangeUserVerifiedCommand.Handler(users));

	@Test
	public void test() {

		User user = new User(Generator.id(), "tester");
		when(users.find(user.getName())).thenReturn(user);

		Command command = new ChangeUserVerifiedCommand(user.asIdentity(), user.getName(), true);
		registry.execute(command);
		assertThat(user.isVerified()).as("user is verified").isTrue();

		Command undo = command.reverse(user.asIdentity());
		registry.execute(undo);
		assertThat(user.isVerified()).as("user is verified").isFalse();

		Command redo = undo.reverse(user.asIdentity());
		registry.execute(redo);
		assertThat(user.isVerified()).as("user is verified").isTrue();
	}
}
