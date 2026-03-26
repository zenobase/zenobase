package com.zenobase.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class ChangeQuotaCommandTest {

	private final UserRepository users = mock(UserRepository.class);
	private final CommandHandlerRegistry registry =
			CommandHandlerRegistry.containing(new ChangeQuotaCommand.Handler(users));

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
	public void testChangeNonExistentUser() {

		User user = new User("tester");

		Command command = new ChangeQuotaCommand(user.asIdentity(), user.getName(), null, 50000);
		assertThatThrownBy(() -> registry.execute(command)).isInstanceOf(NonExistentUserException.class);
	}
}
