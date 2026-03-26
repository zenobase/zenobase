package com.zenobase.services;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandParserRegistry;
import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.models.CommandList;
import com.zenobase.models.Identity;

public class CommandRepository extends RepositorySupport<Command> {

	private static final Logger logger = LoggerFactory.getLogger(CommandRepository.class);

	private static final String INDEX_NAME = "journal";

	private final Index index;
	private final CommandParserRegistry parsers;

	@Inject
	public CommandRepository(IndexManager manager, CommandParserRegistry parsers) {
		this.parsers = parsers;
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			logger.info("Creating journal index...");
			index.create(2);
			index.putMapping(Command.getSchema());
		}
	}

	public void put(Command command) {
		index.store(command.getId(), command.toJson(), DateTime.now(DateTimeZone.UTC), false);
	}

	public @Nullable Command find(String id) {
		ObjectNode node = index.get(id);
		return node != null ? toObject(node) : null;
	}

	public void find(CommandQuery query, SearchOrder order, Callback<Command> callback) {
		SearchRequest.Builder builder = new SearchRequest.Builder()
				.index(index.getIndexName())
				.query(query.build())
				.size(100);
		order.apply(builder);
		index.find(builder, node -> callback.call(toObject(node)));
	}

	public PartialList<Command> find(CommandQuery query, SearchOrder order, int offset, int limit) {
		SearchRequest.Builder builder = new SearchRequest.Builder()
				.index(index.getIndexName())
				.query(query.build())
				.from(offset)
				.size(limit)
				.trackTotalHits(t -> t.enabled(true));
		order.apply(builder);
		return new CommandList(index.find(builder.build()), parsers);
	}

	public long size() {
		return index.count();
	}

	public void refresh() {
		index.refresh();
	}

	public int getTotalCost(Identity principal, DateTime since) {
		String id = "cost";
		Query filter = createFilter(principal, since);
		SearchRequest request = SearchRequest.of(s -> s.index(index.getIndexName())
				.size(1)
				.aggregations(
						id,
						Aggregation.of(a -> a.filter(filter)
								.aggregations(
										id, Aggregation.of(sa -> sa.sum(sum -> sum.field(Command.COST.getName())))))));
		SearchResponse<ObjectNode> response = index.search(request);
		var aggregations = response.aggregations();
		if (aggregations == null) {
			return 0;
		}
		double value = Objects.requireNonNull(Objects.requireNonNull(aggregations.get(id))
						.filter()
						.aggregations()
						.get(id))
				.sum()
				.value();
		return (int) value;
	}

	private static Query createFilter(Identity principal, DateTime since) {
		return Query.of(q -> q.bool(b -> b.must(Query.of(q2 ->
						q2.term(t -> t.field(Command.PRINCIPAL.getName()).value(FieldValue.of(principal.getId())))))
				.must(Query.of(q3 ->
						q3.range(r -> r.field(Command.TIMESTAMP.getName()).gte(JsonData.of(since.getMillis())))))));
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
