package com.zenobase.services;

import java.util.List;

import javax.inject.Inject;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType;
import org.joda.time.DateTime;
import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
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
			index.putMapping(Event.getSchema());
		}
	}

	public void add(String bucketId, Event event, DateTime timestamp) {
		event.prePersist(bucketId);
		getIndex(bucketId).store(Event.TYPE_NAME, event.getId(), event.toJson(), timestamp, false);
		event.postPersist();
	}

	public void update(String bucketId, Event event, DateTime timestamp) {
		event.prePersist(bucketId);
		getIndex(bucketId).update(Event.TYPE_NAME, event.getId(), event.toJson(), timestamp, false);
		event.postPersist();
	}

	public boolean delete(String bucketId, String eventId) {
		return getIndex(bucketId).delete(Event.TYPE_NAME, eventId, false);
	}

	public Event find(String bucketId, String eventId) {
		ObjectNode node = getIndex(bucketId).get(Event.TYPE_NAME, eventId);
		return node != null ? new Event(node) : null;
	}

	public ObjectNode find(String bucketId, Search search) {
		return search.execute(getIndex(bucketId));
	}

	public List<String> terms(String bucketId, String field) {
		final int limit = 100;
		final String facetId = "terms";
		SearchSourceBuilder search = new SearchSourceBuilder().field(field)
			.facet(FacetBuilders.termsFacet(facetId).field(field).size(limit).order(ComparatorType.COUNT));
		SearchResponse response = getIndex(bucketId).search(search);
		List<String> terms = Lists.newArrayList();
		TermsFacet facet = response.getFacets().facet(TermsFacet.class, facetId);
		for (TermsFacet.Entry entry : facet.getEntries()) {
			terms.add(entry.getTerm().toString());
		}
		return terms;
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
