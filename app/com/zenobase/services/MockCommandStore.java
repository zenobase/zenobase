package com.zenobase.services;

import java.util.LinkedHashMap;
import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;

public class MockCommandStore implements CommandStore {

	private final CommandParserRegistry parsers;
	private final LinkedHashMap<String, ObjectNode> history = Maps.newLinkedHashMap();

	@Inject
	public MockCommandStore(CommandParserRegistry parsers) {
		this.parsers = parsers;
	}

	@Override
	public void put(Command command) {
		history.put(command.getId(), command.toJson());
	}

	@Override
	public Command find(String id) {
		ObjectNode commandNode = history.get(id);
		return commandNode != null ? parsers.parse(commandNode) : null;
	}

	@Override
	public void findAll(Callback<Command> callback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PartialList<Command> getHistory(int offset, int limit) {
		List<Command> commands = Lists.newArrayListWithExpectedSize(limit);
		if (offset < size()) {
			int from = offset;
			int to = Math.min(offset + limit, Ints.checkedCast(size()));
			for (ObjectNode node : ImmutableList.copyOf(history.values()).reverse().subList(from, to)) {
				commands.add(parsers.parse(node));
			}
		}
		return new PartialList<Command>(commands, size());
	}

	@Override
	public long size() {
		return history.size();
	}
}
