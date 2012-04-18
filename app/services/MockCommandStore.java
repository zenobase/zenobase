package services;

import java.util.LinkedHashMap;
import java.util.List;

import org.codehaus.jackson.node.ObjectNode;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import commands.Command;
import commands.CommandParserRegistry;
import common.Callback;
import common.PartialList;

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
		int from = Ints.checkedCast(Math.min(offset, size()));
		int to = Ints.checkedCast(Math.min(offset + limit, size()));
		for (int i = to - 1; i >= from; --i) {
			commands.add(parsers.parse(Iterables.get(history.values(), i)));
		}
		return new PartialList<Command>(commands, size());
	}

	@Override
	public long size() {
		return history.size();
	}
}
