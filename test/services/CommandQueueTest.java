package services;

import junit.framework.Assert;

import org.junit.Test;

import secure.Identity;

import commands.Command;
import commands.CommandSupport;

public class CommandQueueTest {

	@Test
	public void test() {

		CommandQueue queue = new CommandQueue();
		Assert.assertEquals(0, queue.size());
		Assert.assertEquals(0, queue.getHistory(0, 2).size());
		Assert.assertEquals(0, queue.getHistory(2, 4).size());

		Command c1 = new MockCommand("first");
		Command c2 = new MockCommand("second");
		Command c3 = new MockCommand("third");

		queue.execute(c1);
		Assert.assertEquals(1, queue.size());
		Assert.assertEquals(1, queue.getHistory(0, 2).size());
		Assert.assertEquals(0, queue.getHistory(2, 4).size());
		Assert.assertSame(c1, queue.getHistory(0, 2).get(0));

		queue.execute(c2);
		queue.execute(c3);
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
			super(new Identity("me"));
			this.label = label;
		}

		public void execute() {
			
		}

		public Command reverse(Identity identity) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return label;
		}
	}
}
