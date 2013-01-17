package com.zenobase.services;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.Logger;
import com.google.inject.Inject;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.models.CommandList;

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
		index.store(Command.TYPE_NAME, command.getId(), command.toJson(), DateTime.now(DateTimeZone.UTC), false);
	}

	public Command find(String id) {
		ObjectNode node = index.get(Command.TYPE_NAME, id);
		return node != null ? toObject(node) : null;
	}

	public void find(Callback<Command> callback) {
		long count = 0;
		for (int offset = 0, limit = 10; ; offset += limit) {
			PartialList<Command> commands = find(offset, limit, false);
			for (Command command : commands) {
				callback.call(command);
				++count;
			}
			if (commands.getTotal() == count) {
				break;
			}
		}
	}

	public PartialList<Command> find(int offset, int limit, boolean newestFirst) {
		return find(newSearchSource(newestFirst).from(offset).size(limit), limit);
	}

	public PartialList<Command> find(String field, String value, int offset, int limit, boolean newestFirst) {
		return find(newSearchSource(field, value, newestFirst).from(offset).size(limit), limit);
	}

	private CommandList find(SearchSourceBuilder search, int limit) {
		return new CommandList(index.find(search), parsers);
	}

	private Command toObject(ObjectNode node) {
    	return parsers.parse(node);
    }

	private static SearchSourceBuilder newSearchSource(boolean newestFirst) {
		return newSearchSource(QueryBuilders.matchAllQuery(), newestFirst);
	}

	private static SearchSourceBuilder newSearchSource(String field, String value, boolean newestFirst) {
		return newSearchSource(QueryBuilders.termQuery(field, value), newestFirst);
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
