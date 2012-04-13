package services;

import java.util.Set;

import junit.framework.Assert;

import org.elasticsearch.common.collect.Sets;
import org.junit.Test;

import models.Identity;

import commands.Command;
import commands.CommandHandler;
import commands.CommandHandlerSupport;
import commands.CommandSupport;

public class CommandQueueTest {

	@Test
	public void test() {

		Set<CommandHandler<?>> handlers = Sets.<CommandHandler<?>>newHashSet(new MockCommandHandler());
		CommandQueue queue = new CommandQueue(handlers);
		Assert.assertEquals(0, queue.size());
		Assert.assertEquals(0, queue.getHistory(0, 2).size());
		Assert.assertEquals(0, queue.getHistory(2, 4).size());

		Command c1 = new MockCommand("first");
		Command c2 = new MockCommand("second");
		Command c3 = new MockCommand("third");

		queue.dispatch(c1);
		Assert.assertEquals(1, queue.size());
		Assert.assertEquals(1, queue.getHistory(0, 2).size());
		Assert.assertEquals(0, queue.getHistory(2, 4).size());
		Assert.assertSame(c1, queue.getHistory(0, 2).get(0));

		queue.dispatch(c2);
		queue.dispatch(c3);
		Assert.assertEquals(3, queue.size());
		Assert.assertEquals(2, queue.getHistory(0, 2).size());
		Assert.assertEquals(1, queue.getHistory(2, 4).size());
		Assert.assertSame(c3, queue.getHistory(0, 2).get(0));
		Assert.assertSame(c2, queue.getHistory(0, 2).get(1));
		Assert.assertSame(c1, queue.getHistory(2, 4).get(0));
	}

	private static class MockCommand extends CommandSupport {

		private final String label;

		public MockCommand(String label) {
			super("mock", new Identity("me"));
			this.label = label;
		}

		@Override
		public Command reverse(Identity identity) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return label;
		}
	}

	private static class MockCommandHandler extends CommandHandlerSupport<MockCommand> {

		public MockCommandHandler() {
			super(MockCommand.class);
		}

		@Override
		public void execute(MockCommand command) {
			
		}
	}
}
