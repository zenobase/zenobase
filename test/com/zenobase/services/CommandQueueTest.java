package com.zenobase.services;

import java.util.Set;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import com.google.common.collect.Sets;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandHandler;
import com.zenobase.commands.CommandHandlerSupport;
import com.zenobase.commands.CommandParser;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.CommandParserSupport;
import com.zenobase.commands.CommandSupport;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class CommandQueueTest {

	@Test
	public void test() {

		Set<CommandHandler<?>> handlers = Sets.<CommandHandler<?>>newHashSet(new MockCommand.Handler());
		Set<CommandParser> parsers = Sets.<CommandParser>newHashSet(new MockCommand.Parser());
		CommandStore store = new MockCommandStore(new CommandParserRegistry(parsers));
		CommandQueue queue = new CommandQueue(new CommandHandlerRegistry(handlers), store);
		Assert.assertEquals(0, store.size());
		Assert.assertEquals(0, store.getHistory(0, 2).size());
		Assert.assertEquals(0, store.getHistory(2, 4).size());

		Command c1 = new MockCommand("first");
		Command c2 = new MockCommand("second");
		Command c3 = new MockCommand("third");

		queue.dispatch(c1);
		Assert.assertEquals(1, store.size());
		Assert.assertEquals(1, store.getHistory(0, 2).getElements().size());
		Assert.assertEquals(0, store.getHistory(2, 4).getElements().size());
		Assert.assertEquals(c1, store.getHistory(0, 2).getElements().get(0));

		queue.dispatch(c2);
		queue.dispatch(c3);
		Assert.assertEquals(3, store.size());
		Assert.assertEquals(2, store.getHistory(0, 2).getElements().size());
		Assert.assertEquals(1, store.getHistory(2, 4).getElements().size());
		Assert.assertEquals(c3, store.getHistory(0, 2).getElements().get(0));
		Assert.assertEquals(c2, store.getHistory(0, 2).getElements().get(1));
		Assert.assertEquals(c1, store.getHistory(2, 4).getElements().get(0));
	}

	private static class MockCommand extends CommandSupport {

		private static final Command.Type TYPE = new Command.Type("mock me", 1);
		private static final TokenField LABEL = new TokenField("label");

		private MockCommand(ObjectNode node) {
			super(node);
		}

		public MockCommand(String label) {
			super(TYPE, new Identity("me"));
			setParameter(LABEL, label);
		}

		private String getLabel() {
			return getParameter(LABEL);
		}

		@Override
		public Command reverse(Identity principal) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int hashCode() {
			return getLabel().hashCode();
		}

		@Override
		public boolean equals(Object that) {
			return that instanceof MockCommand &&
				equals((MockCommand) that);
		}

		private boolean equals(MockCommand that) {
			return getLabel().equals(that.getLabel());
		}

		@Override
		public String toString() {
			return getParameter(LABEL);
		}

		static class Handler extends CommandHandlerSupport<MockCommand> {

			public Handler() {
				super(MockCommand.class);
			}

			@Override
			public void executeTyped(MockCommand command) {

			}
		}

		static class Parser extends CommandParserSupport {
			@Override
			public String getTypeName() {
				return TYPE.getName();
			}
			@Override
			public Command parse(ObjectNode node, int version) {
				Assert.assertEquals("Command type version", TYPE.getVersion(), version);
				return new MockCommand(node);
			}
		}
	}
}
