package com.zenobase.services;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.HasAggregations;
import org.opensearch.search.aggregations.metrics.Sum;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.Logger;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.models.CommandList;
import com.zenobase.models.Identity;

public class CommandRepository extends RepositorySupport<Command> {

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

	public void find(CommandQuery query, SearchOrder order, Callback<Command> callback) {
		long count = 0;
		for (int offset = 0, limit = 100; ; offset += limit) {
			PartialList<Command> commands = find(query, order, offset, limit);
			for (Command command : commands) {
				callback.call(command);
				++count;
			}
			if (commands.getTotal() == count) {
				break;
			}
		}
	}

	public PartialList<Command> find(CommandQuery query, SearchOrder order, int offset, int limit) {
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query.build()).from(offset).size(limit);
		order.apply(search);
		return new CommandList(index.find(search), parsers);
	}

	public long size() {
		return index.count();
	}

	public void refresh() {
		index.refresh();
	}

	public int getTotalCost(Identity principal, DateTime since) {
		String id = "cost";
		AggregationBuilder filter = AggregationBuilders.filter(id, createFilter(principal, since))
			.subAggregation(AggregationBuilders.sum(id).field(Command.COST.getName()));
		SearchSourceBuilder search = new SearchSourceBuilder().size(1).aggregation(filter);
		SearchResponse response = index.search(search);
		Sum sum = ((HasAggregations) response.getAggregations().get(id)).getAggregations().get(id);
		return (int) sum.getValue();
	}

	private static QueryBuilder createFilter(Identity principal, DateTime since) {
		return QueryBuilders.boolQuery()
			.must(QueryBuilders.termQuery(Command.PRINCIPAL.getName(), principal.getId()))
			.must(QueryBuilders.rangeQuery(Command.TIMESTAMP.getName()).from(since.getMillis()));
	}

	@Override
	protected Index getIndex() {
		return index;
	}

	@Override
	protected Command toObject(ObjectNode node) {
    	return parsers.parse(node);
    }
}
