package com.zenobase.services;

import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import play.Logger;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.models.CommandList;
import com.zenobase.models.Identity;

public class CommandRepository {

	private static final String INDEX_NAME = "journal";

	private final Index index;
	private final CommandParserRegistry parsers;

	@Inject
	public CommandRepository(IndexManager manager, CommandParserRegistry parsers) {
		this.parsers = parsers;
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			Logger.info("Creating journal index...");
			index.create(2);
			index.putMapping(Command.getSchema());
		}
	}

	public void put(Command command) {
		index.store(Command.TYPE_NAME, command.getId(), command.toJson(), false);
	}

	public Command find(String id) {
		ObjectNode node = index.get(Command.TYPE_NAME, id);
		return node != null ? parsers.parse(node) : null;
	}

	public void findAll(Callback<Command> callback) {
		long count = 0;
		for (int offset = 0, limit = 10; ; offset += limit) {
			CommandList commands = findAll(offset, limit, false);
			for (Command command : commands.getElements()) {
				callback.call(command);
				++count;
			}
			if (commands.size() == count) {
				break;
			}
		}
	}

	public CommandList findAll(int offset, int limit, boolean newestFirst) {
		return find(newSearchSource(newestFirst).from(offset).size(limit), limit);
	}

	public CommandList find(Identity principal, int offset, int limit, boolean newestFirst) {
		return find(newSearchSource(principal, newestFirst).from(offset).size(limit), limit);
	}

	public CommandList find(SearchSourceBuilder search, int limit) {
		List<Command> commands = Lists.newArrayListWithCapacity(limit);
		PartialList<ObjectNode> hits = index.find(search);
		for (ObjectNode hit : hits.getElements()) {
			commands.add(parsers.parse(hit));
		}
		return new CommandList(commands, hits.size());
	}

	private static SearchSourceBuilder newSearchSource(boolean newestFirst) {
		return newSearchSource(QueryBuilders.matchAllQuery(), newestFirst);
	}

	private static SearchSourceBuilder newSearchSource(Identity principal, boolean newestFirst) {
		return newSearchSource(QueryBuilders.termQuery(Command.PRINCIPAL.getName(), principal.getId()), newestFirst);
	}

	private static SearchSourceBuilder newSearchSource(QueryBuilder query, boolean newestFirst) {
		return new SearchSourceBuilder().query(query)
			.sort(Command.TIMESTAMP.getName(), newestFirst ? SortOrder.DESC : SortOrder.ASC);
	}

	public long size() {
		return index.count();
	}

	public void refresh() {
		index.refresh();
	}
}
