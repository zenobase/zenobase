package com.zenobase.services;

import static com.zenobase.test.NodeAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.models.Identity;
import com.zenobase.models.User;

public class CommandStoreTest extends ElasticSearchTestSupport {

	private final Identity principal = new Identity();

	@Test
	public void test() {

		CommandParserRegistry parsers = CommandParserRegistry.create(new CreateUserCommand.Parser());
		IndexManager indexManager = mock(IndexManager.class);
		Index index = new Index(CommandStore.INDEX_NAME, getClient());
		when(indexManager.getIndex(CommandStore.INDEX_NAME)).thenReturn(index);
		CommandStore store = new CommandStore(indexManager, parsers);
		assertThat(store.size()).as("stored commands").isZero();

		Command command = new CreateUserCommand(principal, new User(principal.getId(), "tester"));
		store.put(command);
		assertThat(store.find(command.getId()).toJson()).isEqualTo(command.toJson());
		index.refresh();
		assertThat(store.size()).as("stored commands").isEqualTo(1);
	}
}
