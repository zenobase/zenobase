package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.TestCommand;
import com.zenobase.common.Callback;
import com.zenobase.models.Identity;
import com.zenobase.testing.PartialListAssert;

public class CommandRepositoryTest extends OpenSearchTestSupport {

	private final Identity principal = new Identity();

	private CommandRepository repository;

	@Before
	public void setUp() {
		CommandParserRegistry parsers = CommandParserRegistry.containing(new TestCommand.Parser());
		repository = new CommandRepository(getManager(), parsers);
	}

	@Test
	public void test() {

		assertThat(repository.size()).as("stored commands").isZero();
		assertThat(repository.getTotalCost(principal, DateTime.now().minusHours(1)))
				.as("cost")
				.isZero();

		Command command1 = new TestCommand(principal, "some work");
		assertThat(repository.find(command1.getId())).isNull();
		repository.put(command1);
		assertThat(repository.find(command1.getId()).toJson()).isEqualTo(command1.toJson());
		repository.refresh();
		assertThat(repository.size()).as("stored commands").isEqualTo(1);
		assertThat(repository.getTotalCost(principal, DateTime.now().minusHours(1)))
				.as("cost")
				.isEqualTo(1);

		Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS); // to ensure correct sort order
		Command command2 = new TestCommand(principal, "more work");
		assertThat(repository.find(command2.getId())).isNull();
		repository.put(command2);
		assertThat(repository.find(command2.getId()).toJson()).isEqualTo(command2.toJson());
		repository.refresh();
		assertThat(repository.size()).as("stored commands").isEqualTo(2);
		assertThat(repository.getTotalCost(principal, DateTime.now().minusHours(1)))
				.as("cost")
				.isEqualTo(2);
		assertThat(repository.getTotalCost(new Identity(), DateTime.now().minusHours(1)))
				.as("cost for different user")
				.isZero();
		assertThat(repository.getTotalCost(principal, DateTime.now().plus(1L)))
				.as("cost since now")
				.isZero();

		PartialListAssert.assertThat(repository.find(new CommandQuery(), CommandQuery.DEFAULT_ORDER, 0, 10))
				.hasTotal(2)
				.isEqualTo(List.of(command2, command1));
	}

	@Test
	public void testFindWithPaging() {
		SearchOrder order = CommandQuery.DEFAULT_ORDER;
		List<Command> expected = Lists.reverse(insert(11));
		assertThat(repository.find(new CommandQuery(), order, 0, 10))
				.hasTotal(expected.size())
				.isEqualTo(expected.subList(0, 10));
		assertThat(repository.find(new CommandQuery(), order, 10, 10))
				.hasTotal(expected.size())
				.isEqualTo(expected.subList(10, 11));
		assertThat(repository.find(new CommandQuery(), order, 20, 10))
				.hasTotal(expected.size())
				.isEmpty();
	}

	@Test
	public void testFindWithPagingInReverse() {
		SearchOrder order = CommandQuery.DEFAULT_ORDER.reverse();
		List<Command> expected = insert(11);
		assertThat(repository.find(new CommandQuery(), order, 0, 10))
				.hasTotal(expected.size())
				.isEqualTo(expected.subList(0, 10));
		assertThat(repository.find(new CommandQuery(), order, 10, 10))
				.hasTotal(expected.size())
				.isEqualTo(expected.subList(10, 11));
		assertThat(repository.find(new CommandQuery(), order, 20, 10))
				.hasTotal(expected.size())
				.isEmpty();
	}

	@Test
	public void testFindWithCallback() {
		List<Command> expected = insert(11);
		Callback<Command> callback = mock(Callback.class);
		repository.find(new CommandQuery(), CommandQuery.DEFAULT_ORDER, callback);
		verifyInteractions(callback, expected);
	}

	@Test
	public void testFindPrincipalEqualTo() {
		Command expected = insert(principal);
		insert(new Identity());
		Callback<Command> callback = mock(Callback.class);
		repository.find(
				new CommandQuery().principalEqualTo(expected.getPrincipal()), CommandQuery.DEFAULT_ORDER, callback);
		verifyInteractions(callback, List.of(expected));
	}

	private List<Command> insert(int size) {
		List<Command> commands = Lists.newArrayListWithCapacity(size);
		for (int i = 0; i < size; ++i) {
			Command command = new TestCommand(principal, "testing");
			commands.add(command);
			Uninterruptibles.sleepUninterruptibly(
					5, TimeUnit.MILLISECONDS); // sleep so we can sort by creation time later
			repository.put(command);
		}
		repository.refresh();
		return commands;
	}

	private Command insert(Identity principal) {
		Command command = new TestCommand(principal, "testing");
		repository.put(command);
		repository.refresh();
		return command;
	}
}
