package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.TestCommand;
import com.zenobase.models.Identity;

public class CommandRepositoryTest extends ElasticSearchTestSupport {

	private final Identity principal = new Identity();

	@Test
	public void test() {

		CommandParserRegistry parsers = CommandParserRegistry.containing(new TestCommand.Parser());
		CommandRepository repository = new CommandRepository(getManager(), parsers);
		assertThat(repository.size()).as("stored commands").isZero();

		Command command1 = new TestCommand(principal, "some work");
		repository.put(command1);
		assertThat(repository.find(command1.getId()).toJson()).isEqualTo(command1.toJson());
		repository.refresh();
		assertThat(repository.size()).as("stored commands").isEqualTo(1);

		Command command2 = new TestCommand(principal, "more work");
		repository.put(command2);
		assertThat(repository.find(command2.getId()).toJson()).isEqualTo(command2.toJson());
		repository.refresh();
		assertThat(repository.size()).as("stored commands").isEqualTo(2);
	}
}
