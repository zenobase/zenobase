package services;

import java.util.Set;

import junit.framework.Assert;
import models.Identity;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

import schema.TokenField;

import com.google.common.collect.Sets;
import commands.Command;
import commands.CommandHandler;
import commands.CommandHandlerSupport;
import commands.CommandParser;
import commands.CommandParserRegistry;
import commands.CommandParserSupport;
import commands.CommandSupport;

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
		Assert.assertSame(c1, store.getHistory(0, 2).getElements().get(0));

		queue.dispatch(c2);
		queue.dispatch(c3);
		Assert.assertEquals(3, store.size());
		Assert.assertEquals(2, store.getHistory(0, 2).size());
		Assert.assertEquals(1, store.getHistory(2, 4).size());
		Assert.assertSame(c3, store.getHistory(0, 2).getElements().get(0));
		Assert.assertSame(c2, store.getHistory(0, 2).getElements().get(1));
		Assert.assertSame(c1, store.getHistory(2, 4).getElements().get(0));
	}

	private static class MockCommand extends CommandSupport {

		private static final TokenField LABEL = new TokenField("label");

		private MockCommand(ObjectNode node) {
			super(node);
		}

		public MockCommand(String label) {
			super("mock", new Identity("me"));
			setParameter(LABEL, label);
		}

		@Override
		public Command reverse(Identity principal) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return getValue(LABEL);
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
			public String getType() {
				return "mock";
			}
			@Override
			public Command parse(ObjectNode node) {
				return new MockCommand(node);
			}
		}
	}
}
