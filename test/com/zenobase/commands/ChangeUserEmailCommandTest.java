package com.zenobase.commands;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.services.CommandHandlerRegistry;
import com.zenobase.services.UserManager;

public class ChangeUserEmailCommandTest {

	private final UserManager users = mock(UserManager.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.create(
		new ChangeUserEmailCommand.Handler(users));

	@Test
	public void test() {

		User user = new User(Generator.id(), "tester");
		when(users.find(user.getName())).thenReturn(user);
		String first = "jdoe@zenobase.org";
		String second = "jdoe@zenobase.com";

		Command command = new ChangeUserEmailCommand(user.asIdentity(), user.getName(), user.getEmail(), first, false, false);
		registry.execute(command);
		assertThat(user.getEmail()).as("email").isEqualTo(first);
		assertThat(user.isVerified()).as("user is verified").isFalse();

		command = new ChangeUserEmailCommand(user.asIdentity(), user.getName(), user.getEmail(), second, false, true);
		registry.execute(command);
		assertThat(user.getEmail()).as("email").isEqualTo(second);
		assertThat(user.isVerified()).as("user is verified").isTrue();

		Command undo = command.reverse(user.asIdentity());
		registry.execute(undo);
		assertThat(user.getEmail()).as("email").isEqualTo(first);
		assertThat(user.isVerified()).as("user is verified").isFalse();

		Command redo = undo.reverse(user.asIdentity());
		registry.execute(redo);
		assertThat(user.getEmail()).as("email").isEqualTo(second);
		assertThat(user.isVerified()).as("user is verified").isTrue();
	}
}
