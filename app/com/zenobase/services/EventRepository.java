package com.zenobase.services;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.MatchAllQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.joda.time.DateTime;
import play.Logger;

import com.zenobase.common.Callback;
import com.zenobase.json.DomainNode;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.search.Search;

public class EventRepository {

	static final String INDEX_NAME = "events";

	private final IndexManager manager;
	private final Index index;

	@Inject
	public EventRepository(IndexManager manager) {
		this.manager = manager;
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			Logger.info("Creating event index...");
			index.create(4, 1);
			index.putMapping(Event.SCHEMA);
		}
	}

	public void add(String bucketId, Event event, DateTime timestamp) {
		event.prePersist(bucketId);
		getIndex(bucketId).store(Event.TYPE_NAME, event.getId(), event.toJson(), timestamp, false);
		event.postPersist();
	}

	public void add(String bucketId, List<Event> events, DateTime timestamp) {
		for (Event event : events) {
			event.prePersist(bucketId);
		}
		getIndex(bucketId).store(Event.TYPE_NAME, events, timestamp, false);
		for (Event event : events) {
			event.postPersist();
		}
	}

	public void update(String bucketId, Event from, Event event, DateTime timestamp) {
		DomainNode.SEQ_NO.setValue(event.toJson(), DomainNode.SEQ_NO.getValue(from.toJson()));
		DomainNode.PRIMARY_TERM.setValue(event.toJson(), DomainNode.PRIMARY_TERM.getValue(from.toJson()));
		event.prePersist(bucketId);
		getIndex(bucketId).update(Event.TYPE_NAME, event.getId(), event.toJson(), timestamp, false);
		event.postPersist();
	}

	public boolean delete(String bucketId, String eventId) {
		return getIndex(bucketId).delete(Event.TYPE_NAME, eventId, false);
	}

	public boolean delete(String bucketId, List<String> eventIds) {
		return getIndex(bucketId).delete(Event.TYPE_NAME, eventIds, false);
	}

	public Event find(String bucketId, String eventId) {
		ObjectNode node = getIndex(bucketId).get(Event.TYPE_NAME, eventId);
		return node != null ? new Event(node) : null;
	}

	public ObjectNode find(String bucketId, Search search) {
		return search.execute(getIndex(bucketId));
	}

	public void find(String bucketId, Search search, Callback<ObjectNode> callback) {
		search.execute(getIndex(bucketId), callback);
	}

	public void findAll(String bucketId, Callback<Event> callback) {
		getIndex(bucketId).find(Query.of(q -> q.matchAll(m -> m)), node -> callback.call(new Event(node)), 100);
	}

	public boolean exists(String bucketId) {
		return getIndex(bucketId).exists();
	}

	public List<String> terms(String bucketId, String field) {
		int limit = 100;
		String id = "terms";
		SearchRequest request = SearchRequest.of(s -> s
			.index(getIndex(bucketId).getIndexName())
			.size(0)
			.aggregations(id, Aggregation.of(a -> a
				.terms(t -> t.field(field).size(limit)
					.order(Collections.singletonMap("_count", SortOrder.Desc)))
			))
		);
		SearchResponse<ObjectNode> response = getIndex(bucketId).search(request);
		List<String> terms = Lists.newArrayList();
		for (StringTermsBucket bucket : response.aggregations().get(id).sterms().buckets().array()) {
			terms.add(bucket.key());
		}
		return terms;
	}

	public long size() {
		return index.count();
	}

	public long size(Identity author) {
		return index.count(Query.of(q -> q.term(t -> t.field(Event.AUTHOR.getName()).value(FieldValue.of(author.getId())))));
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
