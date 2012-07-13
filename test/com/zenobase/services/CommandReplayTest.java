package com.zenobase.services;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.TestCommand;
import com.zenobase.models.Identity;

public class CommandReplayTest extends ElasticSearchTestSupport {

	private final CommandParserRegistry parsers = CommandParserRegistry.containing(new TestCommand.Parser());

	@Test
	public void test() {

		List<Command> commands = newCommands(105);
		addCommands(commands);
		CommandDispatcher dispatcher = Mockito.mock(CommandDispatcher.class);

		new CommandReplay(getClusterName(), getNodeFactory(), parsers, dispatcher).replay(getManager());

		InOrder ordered = Mockito.inOrder(dispatcher);
		for (Command command : commands) {
			ordered.verify(dispatcher).dispatch(command);
		}
	}

	private List<Command> newCommands(int count) {
		List<Command> commands = Lists.newArrayList();
		Identity principal = new Identity();
		for (int i = 0; i < count; ++i) {
			Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS); // to allow buckets to be sorted
			commands.add(new TestCommand(principal, String.format("Command #%s", i + 1)));
		}
		return commands;
	}

	private void addCommands(Iterable<Command> commands) {
		CommandRepository repository = new CommandRepository(getManager(), parsers);
		for (Command command : commands) {
			repository.put(command);
		}
		repository.refresh();
	}
}
