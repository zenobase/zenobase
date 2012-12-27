package com.zenobase.services;

import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.zenobase.models.Event;
import com.zenobase.search.Search;

public class EventRepository {

	private final IndexManager manager;

	@Inject
	public EventRepository(IndexManager manager) {
		this.manager = manager;
	}

	public void add(String bucketId, Event event) {
		event.prePersist();
		getIndex(bucketId).store(Event.TYPE_NAME, event.getId(), event.toJson(), false);
	}

	public void update(String bucketId, Event event) {
		event.prePersist();
		getIndex(bucketId).update(Event.TYPE_NAME, event.getId(), event.toJson(), false);
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
		TermsFacet facet = response.facets().facet(TermsFacet.class, facetId);
		for (TermsFacet.Entry entry : facet.entries()) {
			terms.add(entry.getTerm());
		}
		return terms;
	}

	public long size(String bucketId) {
		return getIndex(bucketId).count();
	}

	public void refresh(String bucketId) {
		getIndex(bucketId).refresh();
	}

	public void close(String bucketId) {
		getIndex(bucketId).close();
	}

	private Index getIndex(String bucketId) {
		return manager.getIndex(bucketId);
	}
}
