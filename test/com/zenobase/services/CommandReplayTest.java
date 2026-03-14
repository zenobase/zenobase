package com.zenobase.services;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.TestCommand;
import com.zenobase.models.Identity;
import com.zenobase.models.User;

import static org.junit.Assert.assertEquals;

public class CommandReplayTest extends OpenSearchTestSupport {

	private final User user = new User("jdoe");
	private final CommandParserRegistry parsers = CommandParserRegistry.containing(new TestCommand.Parser());

	@Test
	public void test() {

		List<Command> commandsToReplay = newCommands(55, user.asIdentity());
		addCommands(commandsToReplay);
		List<Command> commandsToDiscard = newCommands(50, new Identity());
		addCommands(commandsToDiscard);

		CommandDispatcher dispatcher = Mockito.mock(CommandDispatcher.class);

		new UserRepository(getManager()).store(user, DateTime.now());
		new CommandReplay("", 4, parsers, dispatcher)
			.replay(getManager(), new IdentitiesFilterBuilder(
				new UserRepository(getManager()), new AuthorizationRepository(getManager()))
				.deterministic(true));

		InOrder dispatchOrder = Mockito.inOrder(dispatcher);
		for (Command command : commandsToReplay) {
			dispatchOrder.verify(dispatcher).dispatch(command);
		}
		for (Command command : commandsToDiscard) {
			Mockito.verify(dispatcher).discard(command);
		}
		assertEquals(commandsToReplay.size() + commandsToDiscard.size(),
			Mockito.mockingDetails(dispatcher).getInvocations().size());
	}

	private List<Command> newCommands(int count, Identity principal) {
		List<Command> commands = Lists.newArrayList();
		for (int i = 0; i < count; ++i) {
			Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS); // sleep so we can sort by creation time later
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
