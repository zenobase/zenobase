package com.zenobase.commands;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class OptOutCommandTest {

	private final UserRepository users = mock(UserRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new OptOutCommand.Handler(users), new OptInCommand.Handler(users));

	@Test
	public void test() {

		User user = new User("tester");
		when(users.find(user.getName())).thenReturn(user);

		Command command = new OptOutCommand(user.asIdentity(), user.getName());
		registry.execute(command);
		assertThat(user.isOptedOut()).isTrue();

		Command undo = command.reverse(user.asIdentity());
		registry.execute(undo);
		assertThat(user.isOptedOut()).isFalse();

		Command redo = undo.reverse(user.asIdentity());
		registry.execute(redo);
		assertThat(user.isOptedOut()).isTrue();
	}
}
