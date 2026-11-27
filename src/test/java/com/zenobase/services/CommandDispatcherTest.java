package com.zenobase.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandHandlerRegistry;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.TestCommand;
import com.zenobase.models.Identity;
import com.zenobase.repositories.CommandRepository;
import java.time.Duration;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;

public class CommandDispatcherTest {

	static final Identity TESTER = new Identity("tester");

	final CommandHandlerRegistry handlers = mock(CommandHandlerRegistry.class);
	final CommandRepository repository = mock(CommandRepository.class);
	final CommandDispatcher retrying = new CommandDispatcher(
		handlers,
		repository,
		mock(QuotaManager.class),
		List.of(Duration.ZERO, Duration.ZERO)
	);

	@Test
	public void test() {
		CommandHandlerRegistry handlers = mock(CommandHandlerRegistry.class);
		CommandRepository repository = mock(CommandRepository.class);
		QuotaManager quotas = mock(QuotaManager.class);

		CommandDispatcher dispatcher = new CommandDispatcher(handlers, repository, quotas);

		Command c1 = new TestCommand(TESTER, "do a bit");
		Command c2 = new TestCommand(TESTER, "do more");
		Command c3 = new TestCommand(TESTER, "do most").setTimestamp(DateTime.now().minusMonths(1));

		dispatcher.dispatch(c1);
		dispatcher.dispatch(c2);
		dispatcher.dispatch(c3);

		verify(repository).put(c1);
		verify(repository).put(c2);
		verify(repository).put(c3);

		verify(handlers).execute(c1);
		verify(handlers).execute(c2);
		verify(handlers).execute(c3);

		verify(quotas, times(2)).spend(TESTER, 1);
	}

	@Test
	public void testCompoundCommand() {
		CommandHandlerRegistry handlers = mock(CommandHandlerRegistry.class);
		CommandRepository repository = mock(CommandRepository.class);
		QuotaManager quotas = mock(QuotaManager.class);

		CommandDispatcher dispatcher = new CommandDispatcher(handlers, repository, quotas);

		Command c1 = new TestCommand(TESTER, "do a bit");
		Command c2 = new TestCommand(TESTER, "do more");
		Command c3 = new TestCommand(TESTER, "do most");
		CompoundCommand cc = new CompoundCommand(TESTER, "do it all", "undo it all");
		cc.add(c1);
		cc.add(c2);
		cc.add(c3);

		dispatcher.dispatch(cc);

		verify(repository).put(cc);

		verify(handlers).execute(c1);
		verify(handlers).execute(c2);
		verify(handlers).execute(c3);

		verify(quotas).spend(TESTER, 3);
	}

	@Test
	public void testFailingCompoundCommand() {
		CommandHandlerRegistry handlers = mock(CommandHandlerRegistry.class);
		CommandRepository repository = mock(CommandRepository.class);
		QuotaManager quotas = mock(QuotaManager.class);

		CommandDispatcher dispatcher = new CommandDispatcher(handlers, repository, quotas);

		Command c1 = new TestCommand(TESTER, "do a bit");
		Command c2 = new TestCommand(TESTER, "do more");
		Command c3 = new TestCommand(TESTER, "do most");

		doThrow(new RuntimeException()).when(handlers).execute(c3);

		CompoundCommand cc = new CompoundCommand(TESTER, "do it all", "undo it all");
		cc.add(c1);
		cc.add(c2);
		cc.add(c3);

		try {
			dispatcher.dispatch(cc);
			throw new AssertionError("expected an exception");
		} catch (RuntimeException e) {}
		verifyNoInteractions(repository);

		verify(handlers, times(5)).execute(any(TestCommand.class));
	}

	@Test
	public void testJournalRetry() {
		Command command = new TestCommand(TESTER, "abc");
		doThrow(new RuntimeException("timeout")).doNothing().when(repository).put(command);

		retrying.dispatch(command);

		verify(repository, times(2)).put(command);
		verify(handlers).execute(command);
		verifyNoMoreInteractions(handlers);
	}

	@Test
	public void testJournalRetryConflict() {
		Command command = new TestCommand(TESTER, "abc");
		doThrow(new RuntimeException("timeout")).doThrow(conflict()).when(repository).put(command);

		retrying.dispatch(command);

		verify(repository, times(2)).put(command);
		verify(handlers).execute(command);
		verifyNoMoreInteractions(handlers);
	}

	@Test
	public void testJournalConflict() {
		Command command = new TestCommand(TESTER, "abc");
		OpenSearchException conflict = conflict();
		doThrow(conflict).when(repository).put(command);

		assertSame(conflict, assertThrows(OpenSearchException.class, () -> retrying.dispatch(command)));

		verify(repository).put(command);
		verify(handlers).execute(command);
		verifyNoMoreInteractions(handlers);
	}

	@Test
	public void testJournalFailureReverts() {
		Command command = new TestCommand(TESTER, "abc");
		RuntimeException failure = new RuntimeException("down");
		doThrow(failure).when(repository).put(command);

		assertSame(failure, assertThrows(RuntimeException.class, () -> retrying.dispatch(command)));

		verify(repository, times(3)).put(command);
		verify(handlers).execute(command);
		verify(handlers).execute(argThat(c -> c instanceof TestCommand t && t.getTag().equals("cba")));
	}

	@Test
	public void testJournalInterrupted() {
		Command command = new TestCommand(TESTER, "abc");
		doThrow(new RuntimeException("timeout")).doNothing().when(repository).put(command);

		Thread.currentThread().interrupt();
		assertThrows(RuntimeException.class, () -> retrying.dispatch(command));

		assertTrue(Thread.interrupted());
		verify(repository).put(command);
		verify(handlers).execute(argThat(c -> c instanceof TestCommand t && t.getTag().equals("cba")));
	}

	@Test
	public void testJournalFailureIrreversible() {
		Command command = spy(new TestCommand(TESTER, "abc"));
		doThrow(UnsupportedOperationException.class).when(command).reverse(any());
		RuntimeException failure = new RuntimeException("down");
		doThrow(failure).when(repository).put(command);

		assertSame(failure, assertThrows(RuntimeException.class, () -> retrying.dispatch(command)));

		assertInstanceOf(UnsupportedOperationException.class, failure.getSuppressed()[0]);
		verify(repository, times(3)).put(command);
		verify(handlers).execute(command);
		verifyNoMoreInteractions(handlers);
	}

	private static OpenSearchException conflict() {
		return new OpenSearchException(
			ErrorResponse.of(r ->
				r.status(409).error(e -> e.type("version_conflict_engine_exception").reason("version conflict"))
			)
		);
	}
}
