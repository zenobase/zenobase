package com.zenobase.services;

import static com.zenobase.test.NodeAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.TestCommand;
import com.zenobase.models.Identity;

public class CommandStoreTest extends ElasticSearchTestSupport {

	private final Identity principal = new Identity();

	@Test
	public void test() {

		CommandParserRegistry parsers = CommandParserRegistry.containing(new TestCommand.Parser());
		IndexManager indexManager = mock(IndexManager.class);
		Index index = new Index(CommandStore.INDEX_NAME, getClient());
		when(indexManager.getIndex(CommandStore.INDEX_NAME)).thenReturn(index);
		CommandStore store = new CommandStore(indexManager, parsers);
		assertThat(store.size()).as("stored commands").isZero();

		Command command1 = new TestCommand(principal, "some work");
		store.put(command1);
		assertThat(store.find(command1.getId()).toJson()).isEqualTo(command1.toJson());
		index.refresh();
		assertThat(store.size()).as("stored commands").isEqualTo(1);

		Command command2 = new TestCommand(principal, "more work");
		store.put(command2);
		assertThat(store.find(command2.getId()).toJson()).isEqualTo(command2.toJson());
		index.refresh();
		assertThat(store.size()).as("stored commands").isEqualTo(2);
	}
}
