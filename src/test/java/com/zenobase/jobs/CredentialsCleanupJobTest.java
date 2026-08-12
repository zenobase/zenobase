package com.zenobase.jobs;

import static com.zenobase.testing.CallbackAnswer.doCallback;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.zenobase.commands.Command;
import com.zenobase.commands.DeleteCredentialsCommand;
import com.zenobase.models.Identity;
import com.zenobase.queries.CredentialsQuery;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.tasks.Credentials;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class CredentialsCleanupJobTest {

	private final CredentialsRepository credentials = mock(CredentialsRepository.class);
	private final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	private final CredentialsCleanupJob job = new CredentialsCleanupJob(credentials, dispatcher);

	@Test
	public void test() {
		var principal = new Identity();
		var stale = new Credentials("foo", principal, DateTime.now().minusHours(1));

		doCallback(stale).when(credentials).find(any(CredentialsQuery.class), any());

		job.run();

		var command = ArgumentCaptor.forClass(Command.class);
		verify(dispatcher).dispatch(command.capture());
		assertThat(command.getValue()).isInstanceOf(DeleteCredentialsCommand.class);
		assertThat(command.getValue().getPrincipal()).isEqualTo(principal);
	}
}
