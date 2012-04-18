package services;

import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import play.Logger;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import commands.Command;
import commands.CommandParserRegistry;
import commands.CommandSupport;
import common.Callback;
import common.PartialList;

public class PersistentCommandStore implements CommandStore {

	private static final String INDEX_NAME = "queue";

	private final Index index;
	private final CommandParserRegistry parsers;

	@Inject
	public PersistentCommandStore(IndexManager manager, CommandParserRegistry parsers) {
		this.parsers = parsers;
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			Logger.info("Creating queue index...");
			index.create(2);
			index.putMapping(CommandSupport.getSchema());
		}
	}

	@Override
	public void put(Command command) {
		index.store(CommandSupport.TYPE_NAME, command.getId(), command.toJson(), false);
	}

	@Override
	public Command find(String id) {
		ObjectNode node = index.get(CommandSupport.TYPE_NAME, id);
		return node != null ? parsers.parse(node) : null;
	}

	@Override
	public void findAll(Callback<Command> callback) {
		SearchSourceBuilder search = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery())
			.sort(CommandSupport.TIMESTAMP.getName(), SortOrder.ASC).size(Ints.checkedCast(size()));
		for (ObjectNode hit : index.find(search).getElements()) {
			callback.call(parsers.parse(hit));
		}
	}

	@Override
	public PartialList<Command> getHistory(int offset, int limit) {
		List<Command> commands = Lists.newArrayListWithCapacity(limit);
		SearchSourceBuilder search = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery())
			.from(offset).size(limit).sort(CommandSupport.TIMESTAMP.getName(), SortOrder.DESC);
		for (ObjectNode hit : index.find(search).getElements()) {
			commands.add(parsers.parse(hit));
		}
		return new PartialList<Command>(commands, size());
	}

	@Override
	public long size() {
		return index.count();
	}
}
