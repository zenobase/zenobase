package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.junit.Test;
import com.google.common.collect.ImmutableList;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.TestCommand;
import com.zenobase.models.Identity;
import com.zenobase.testing.PartialListAssert;

public class CommandRepositoryTest extends ElasticSearchTestSupport {

	private final Identity principal = new Identity();

	@Test
	public void test() {

		CommandParserRegistry parsers = CommandParserRegistry.containing(new TestCommand.Parser());
		CommandRepository repository = new CommandRepository(getManager(), parsers);
		assertThat(repository.size()).as("stored commands").isZero();
		assertThat(repository.getTotalCost(principal, DateTime.now().minusHours(1))).as("cost").isZero();

		Command command1 = new TestCommand(principal, "some work");
		assertThat(repository.find(command1.getId())).isNull();
		repository.put(command1);
		assertThat(repository.find(command1.getId()).toJson()).isEqualTo(command1.toJson());
		repository.refresh();
		assertThat(repository.size()).as("stored commands").isEqualTo(1);
		assertThat(repository.getTotalCost(principal, DateTime.now().minusHours(1))).as("cost").isEqualTo(1);

		Command command2 = new TestCommand(principal, "more work");
		assertThat(repository.find(command2.getId())).isNull();
		repository.put(command2);
		assertThat(repository.find(command2.getId()).toJson()).isEqualTo(command2.toJson());
		repository.refresh();
		assertThat(repository.size()).as("stored commands").isEqualTo(2);
		assertThat(repository.getTotalCost(principal, DateTime.now().minusHours(1))).as("cost").isEqualTo(2);
		assertThat(repository.getTotalCost(new Identity(), DateTime.now().minusHours(1))).as("cost for different user").isZero();
		assertThat(repository.getTotalCost(principal, DateTime.now())).as("cost since now").isZero();

		PartialListAssert.assertThat(repository.find(0, 10, true)).hasTotal(2).isEqualTo(ImmutableList.of(command2, command1));
	}
}
