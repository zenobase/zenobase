package com.zenobase.services;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.TestCommand;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.repositories.CommandRepository;
import com.zenobase.repositories.OpenSearchTestSupport;
import com.zenobase.repositories.UserRepository;

public class CommandReplayTest extends OpenSearchTestSupport {

	private final User user = new User("jdoe");
	private final User guest = new User("ghost");
	private final CommandParserRegistry parsers = CommandParserRegistry.containing(new TestCommand.Parser());

	@Test
	public void test() {
		user.setEmail("jdoe@example.com");

		List<Command> commandsToReplay = newCommands(55, user.asIdentity());
		addCommands(commandsToReplay);
		List<Command> commandsToDiscard = newCommands(50, new Identity());
		addCommands(commandsToDiscard);
		List<Command> guestCommandsToDiscard = newCommands(25, guest.asIdentity());
		addCommands(guestCommandsToDiscard);

		CommandDispatcher dispatcher = Mockito.mock(CommandDispatcher.class);

		UserRepository users = new UserRepository(getManager());
		users.store(user);
		users.store(guest);
		new CommandReplay("", parsers, dispatcher).replay(getManager());

		InOrder dispatchOrder = Mockito.inOrder(dispatcher);
		for (Command command : commandsToReplay) {
			dispatchOrder.verify(dispatcher).dispatch(command);
		}
		for (Command command : commandsToDiscard) {
			Mockito.verify(dispatcher).discard(command);
		}
		for (Command command : guestCommandsToDiscard) {
			Mockito.verify(dispatcher).discard(command);
		}
		assertEquals(
			commandsToReplay.size() + commandsToDiscard.size() + guestCommandsToDiscard.size(),
			Mockito.mockingDetails(dispatcher).getInvocations().size()
		);
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
