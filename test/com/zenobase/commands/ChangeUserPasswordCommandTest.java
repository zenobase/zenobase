package com.zenobase.commands;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class ChangeUserPasswordCommandTest {

	private final UserRepository users = mock(UserRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new ChangeUserPasswordCommand.Handler(users));

	@Test
	public void test() {

		String from = "secret";
		String to = "s3cr3t";
		User user = new User(Generator.id(), "tester");
		user.setPassword(from);
		when(users.find(user.getName())).thenReturn(user);

		Command command = new ChangeUserPasswordCommand(user.asIdentity(), user.getName(), User.getHashedPassword(from), User.getHashedPassword(to));
		registry.execute(command);
		assertThat(user.passwordEquals(to)).isTrue();

		Command undo = command.reverse(user.asIdentity());
		registry.execute(undo);
		assertThat(user.passwordEquals(from)).isTrue();

		Command redo = undo.reverse(user.asIdentity());
		registry.execute(redo);
		assertThat(user.passwordEquals(to)).isTrue();
	}
}
