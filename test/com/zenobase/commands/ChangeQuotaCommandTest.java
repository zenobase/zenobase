package com.zenobase.commands;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.joda.time.DateTime;
import org.junit.Test;

import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class ChangeQuotaCommandTest {

	private final UserRepository users = mock(UserRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new ChangeQuotaCommand.Handler(users));

	@Test
	public void test() {

		User user = new User("tester");
		when(users.find(user.getName())).thenReturn(user);

		Command command = new ChangeQuotaCommand(user.asIdentity(), user.getName(), null, 50000);
		registry.execute(command);
		assertThat(user.getQuota()).isEqualTo(50000);

		Command undo = command.reverse(user.asIdentity());
		registry.execute(undo);
		assertThat(user.getQuota()).isNull();

		Command redo = undo.reverse(user.asIdentity());
		registry.execute(redo);
		assertThat(user.getQuota()).isEqualTo(50000);
	}

	@Test
	public void testOnMissingUser() {

		User user = new User("tester");
		when(users.find(user.getName())).thenReturn(null);

		Command command = new ChangeQuotaCommand(user.asIdentity(), user.getName(), null, 50000);
		registry.execute(command);

		verify(users, never()).update(any(User.class), any(DateTime.class));
	}
}
