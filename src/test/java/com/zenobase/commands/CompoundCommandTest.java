package com.zenobase.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Identity;

public class CompoundCommandTest {

	private final Identity principal = new Identity();
	private final CompoundCommand command = new CompoundCommand(principal, "do it all", "undo it all");

	@Before
	public void setUp() {
		assertThat(command.getCommands()).isEmpty();
		command.add(new TestCommand(principal, "foo"));
		command.add(new TestCommand(principal, "bar"));
		assertThat(command.getCommands()).hasSize(2);
	}

	@Test
	public void testCopy() {
		CommandParserRegistry registry = new CommandParserRegistry(ImmutableSet.of(new TestCommand.Parser()));
		CompoundCommand copy = new CompoundCommand(command.toJson(), registry);
		assertThat(copy.getCommands()).isEqualTo(command.getCommands());
	}

	@Test
	public void testReverse() {
		CompoundCommand reverse = command.reverse(principal);
		assertThat(reverse.getCommands()).hasSize(command.getCommands().size()).isNotEqualTo(command.getCommands());
	}
}
