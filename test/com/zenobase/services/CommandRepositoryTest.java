package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
		IndexManager indexManager = mock(IndexManager.class);
		Index index = new Index(CommandRepository.INDEX_NAME, getClient());
		when(indexManager.getIndex(CommandRepository.INDEX_NAME)).thenReturn(index);
		CommandRepository repository = new CommandRepository(indexManager, parsers);
		assertThat(repository.size()).as("stored commands").isZero();

		Command command1 = new TestCommand(principal, "some work");
		repository.put(command1);
		assertThat(repository.find(command1.getId()).toJson()).isEqualTo(command1.toJson());
		index.refresh();
		assertThat(repository.size()).as("stored commands").isEqualTo(1);

		Command command2 = new TestCommand(principal, "more work");
		repository.put(command2);
		assertThat(repository.find(command2.getId()).toJson()).isEqualTo(command2.toJson());
		index.refresh();
		assertThat(repository.size()).as("stored commands").isEqualTo(2);
	}
}
