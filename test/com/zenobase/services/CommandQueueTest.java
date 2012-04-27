package com.zenobase.services;

import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandSupport;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.models.Identity;

public class CommandQueueTest {

	private static final Identity TESTER = new Identity("tester");

	@Test
	public void test() {

		CommandHandlerRegistry handlers = mock(CommandHandlerRegistry.class);
		CommandStore store = mock(CommandStore.class);

		CommandQueue queue = new CommandQueue(handlers, store);

		Command c1 = new MockCommand("do a bit");
		Command c2 = new MockCommand("do more");
		Command c3 = new MockCommand("do most");

		queue.dispatch(c1);
		queue.dispatch(c2);
		queue.dispatch(c3);

		verify(store).put(c1);
		verify(store).put(c2);
		verify(store).put(c3);

		verify(handlers).execute(c1);
		verify(handlers).execute(c2);
		verify(handlers).execute(c3);
	}

	@Test
	public void testCompoundCommand() {

		CommandHandlerRegistry handlers = mock(CommandHandlerRegistry.class);
		CommandStore store = mock(CommandStore.class);

		CommandQueue queue = new CommandQueue(handlers, store);

		Command c1 = new MockCommand("do a bit");
		Command c2 = new MockCommand("do more");
		Command c3 = new MockCommand("do most");
		CompoundCommand cc = new CompoundCommand(TESTER, "do it all", "undo it all");
		cc.add(c1);
		cc.add(c2);
		cc.add(c3);

		queue.dispatch(cc);

		verify(store).put(cc);

		verify(handlers).execute(c1);
		verify(handlers).execute(c2);
		verify(handlers).execute(c3);
	}

	private static class MockCommand extends CommandSupport {

		public MockCommand(String typeName) {
			super(new Command.Type(typeName, 1), TESTER);
		}

		@Override
		public Command reverse(Identity principal) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return getType().toString();
		}
	}
}
