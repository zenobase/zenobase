package com.zenobase.services;

import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandHandlerRegistry;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.TestCommand;
import com.zenobase.models.Identity;

public class CommandQueueTest {

	static final Identity TESTER = new Identity("tester");

	@Test
	public void test() {

		CommandHandlerRegistry handlers = mock(CommandHandlerRegistry.class);
		CommandRepository repository = mock(CommandRepository.class);

		CommandQueue queue = new CommandQueue(handlers, repository);

		Command c1 = new TestCommand(TESTER, "do a bit");
		Command c2 = new TestCommand(TESTER, "do more");
		Command c3 = new TestCommand(TESTER, "do most");

		queue.dispatch(c1);
		queue.dispatch(c2);
		queue.dispatch(c3);

		verify(repository).put(c1);
		verify(repository).put(c2);
		verify(repository).put(c3);

		verify(handlers).execute(c1);
		verify(handlers).execute(c2);
		verify(handlers).execute(c3);
	}

	@Test
	public void testCompoundCommand() {

		CommandHandlerRegistry handlers = mock(CommandHandlerRegistry.class);
		CommandRepository repository = mock(CommandRepository.class);

		CommandQueue queue = new CommandQueue(handlers, repository);

		Command c1 = new TestCommand(TESTER, "do a bit");
		Command c2 = new TestCommand(TESTER, "do more");
		Command c3 = new TestCommand(TESTER, "do most");
		CompoundCommand cc = new CompoundCommand(TESTER, "do it all", "undo it all");
		cc.add(c1);
		cc.add(c2);
		cc.add(c3);

		queue.dispatch(cc);

		verify(repository).put(cc);

		verify(handlers).execute(c1);
		verify(handlers).execute(c2);
		verify(handlers).execute(c3);
	}
}
