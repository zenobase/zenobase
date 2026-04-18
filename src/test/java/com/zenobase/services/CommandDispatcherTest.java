package com.zenobase.services;

import static org.mockito.Mockito.*;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandHandlerRegistry;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.TestCommand;
import com.zenobase.models.Identity;
import com.zenobase.repositories.CommandRepository;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommandDispatcherTest {

	static final Identity TESTER = new Identity("tester");

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
}
