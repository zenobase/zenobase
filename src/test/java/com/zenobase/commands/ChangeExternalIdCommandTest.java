package com.zenobase.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zenobase.models.User;
import com.zenobase.repositories.UserRepository;
import org.junit.jupiter.api.Test;

public class ChangeExternalIdCommandTest {

	private final UserRepository users = mock(UserRepository.class);
	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new ChangeExternalIdCommand.Handler(users)
	);

	@Test
	public void test() {
		User user = new User("tester");
		when(users.find(user.getName())).thenReturn(user);

		Command command = new ChangeExternalIdCommand(user.asIdentity(), user.getName(), "auth0|abc");
		registry.execute(command);
		assertThat(user.getExternalId()).as("external id").isEqualTo("auth0|abc");
		assertThat(command.toString()).isEqualTo("set external id for tester");

		Command undo = command.reverse(user.asIdentity());
		registry.execute(undo);
		assertThat(user.getExternalId()).as("external id after undo").isEqualTo("");
		verify(users, times(2)).update(user);
	}
}
