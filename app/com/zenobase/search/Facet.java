package com.zenobase.search;

import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public abstract class Facet {

	private final String id;

	protected Facet(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public abstract void configure(SearchSourceBuilder request);

	public abstract JsonNode process(SearchResponse response);

	@Override
	public boolean equals(Object that) {
		return that instanceof Facet &&
			id.equals(((Facet) that).getId());
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
