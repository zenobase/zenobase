package com.zenobase.jobs;

import static com.zenobase.testing.CallbackAnswer.doCallback;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.zenobase.commands.Command;
import com.zenobase.commands.SuspendUserCommand;
import com.zenobase.models.User;
import com.zenobase.queries.UserQuery;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class UnverifiedEmailSuspensionJobTest {

	private final UserRepository users = mock(UserRepository.class);
	private final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	private final UnverifiedEmailSuspensionJob job = new UnverifiedEmailSuspensionJob(users, dispatcher);

	@Test
	public void test() {
		User user = new User("stale-tester");

		doCallback(user).when(users).find(any(UserQuery.class), any());

		job.run();

		var command = ArgumentCaptor.forClass(Command.class);
		verify(dispatcher).dispatch(command.capture());
		assertThat(command.getValue()).isInstanceOf(SuspendUserCommand.class);
		assertThat(command.getValue().getPrincipal()).isEqualTo(user.asIdentity());
	}
}
