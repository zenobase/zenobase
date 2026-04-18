package com.zenobase.repositories;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.common.Callback;
import com.zenobase.json.DomainNode;
import com.zenobase.json.Field;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.search.Search;
import com.zenobase.services.SearchOrder;
import jakarta.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventRepository {

	private static final Logger logger = LoggerFactory.getLogger(EventRepository.class);

	static final String INDEX_NAME = "events";

	private final IndexManager manager;
	private final Index index;

	@Inject
	public EventRepository(IndexManager manager) {
		this.manager = manager;
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			logger.info("Creating event index...");
			index.create(4, 1);
			index.putMapping(Event.SCHEMA);
		}
	}

	public void add(String bucketId, Event event) {
		event.prePersist(bucketId);
		getIndex(bucketId).store(event, false);
		event.postPersist();
	}

	public void add(String bucketId, List<Event> events) {
		for (Event event : events) {
			event.prePersist(bucketId);
		}
		getIndex(bucketId).store(events, false);
		for (Event event : events) {
			event.postPersist();
		}
	}

	public void update(String bucketId, Event from, Event event) {
		event.setOptimisticLock(Objects.requireNonNull(from.getOptimisticLock()));
		event.prePersist(bucketId);
		getIndex(bucketId).update(event, false);
		event.postPersist();
	}

	public boolean delete(String bucketId, String eventId) {
		return getIndex(bucketId).delete(eventId, false);
	}

	public boolean delete(String bucketId, List<String> eventIds) {
		return getIndex(bucketId).delete(eventIds, false);
	}

	public @Nullable Event find(String bucketId, String eventId) {
		ObjectNode node = getIndex(bucketId).get(eventId);
		return node != null ? new Event(node) : null;
	}

	public ObjectNode find(String bucketId, Search search) {
		return search.execute(getIndex(bucketId));
	}

	public void find(String bucketId, Search search, Callback<ObjectNode> callback) {
		search.execute(getIndex(bucketId), callback);
	}

	public void findAll(String bucketId, Callback<Event> callback) {
		getIndex(bucketId).find(
			Query.of(q -> q.matchAll(m -> m)),
			SearchOrder.asc(Event.ID),
			node -> callback.call(new Event(node)),
			1000
		);
	}

	public List<String> terms(String bucketId, String field) {
		int limit = 100;
		String id = "terms";
		SearchRequest request = SearchRequest.of(s ->
			s
				.index(getIndex(bucketId).getIndexName())
				.size(0)
				.aggregations(
					id,
					Aggregation.of(a ->
						a.terms(t ->
							t.field(field).size(limit).order(Collections.singletonMap("_count", SortOrder.Desc))
						)
					)
				)
		);
		SearchResponse<ObjectNode> response = getIndex(bucketId).search(request);
		List<String> terms = new ArrayList<>();
		var aggregation = response.aggregations().get(id);
		if (aggregation == null) {
			return terms;
		}
		for (StringTermsBucket bucket : aggregation.sterms().buckets().array()) {
			terms.add(bucket.key());
		}
		return terms;
	}

	public List<Field<?>> fields(String bucketId) {
		Map<String, Aggregation> aggregations = new LinkedHashMap<>();
		Map<String, Field<?>> fieldsByAggName = new LinkedHashMap<>();
		int i = 0;
		for (Field<?> field : Event.FIELDS) {
			if (field == Event.BUCKET || field == DomainNode.VERSION) {
				continue;
			}
			String aggName = "f" + i++;
			String fieldName = field.getName();
			aggregations.put(aggName, Aggregation.of(a -> a.filter(q -> q.exists(e -> e.field(fieldName)))));
			fieldsByAggName.put(aggName, field);
		}
		SearchRequest request = SearchRequest.of(s ->
			s.index(getIndex(bucketId).getIndexName()).size(0).aggregations(aggregations)
		);
		SearchResponse<ObjectNode> response = getIndex(bucketId).search(request);
		Map<Field<?>, Long> counts = new LinkedHashMap<>();
		for (Map.Entry<String, Field<?>> entry : fieldsByAggName.entrySet()) {
			Aggregate aggregate = response.aggregations().get(entry.getKey());
			long count = aggregate != null ? aggregate.filter().docCount() : 0L;
			if (count > 0) {
				counts.put(entry.getValue(), count);
			}
		}
		return counts
			.entrySet()
			.stream()
			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			.map(Map.Entry::getKey)
			.collect(Collectors.toUnmodifiableList());
	}

	public long size() {
		return index.count();
	}

	public long size(Identity author) {
		return index.count(
			Query.of(q -> q.term(t -> t.field(Event.AUTHOR.getName()).value(FieldValue.of(author.id()))))
		);
	}

	public long size(String bucketId) {
		return getIndex(bucketId).count();
	}

	public void refresh(String bucketId) {
		getIndex(bucketId).refresh();
	}

	private Index getIndex(String bucketId) {
		return manager.getIndex(bucketId);
	}
}
