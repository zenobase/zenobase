package com.zenobase.search;

import org.codehaus.jackson.JsonNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public abstract class Widget {

	private final String id;

	protected Widget(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public abstract void configure(SearchSourceBuilder request);

	public abstract JsonNode process(SearchResponse response);

	@Override
	public boolean equals(Object that) {
		return that instanceof Widget &&
			id.equals(((Widget) that).getId());
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
