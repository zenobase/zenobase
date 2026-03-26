package com.zenobase.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

public abstract class Facet {

	private final String id;

	protected Facet(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public abstract void configure(SearchRequest.Builder request);

	public abstract JsonNode process(SearchResponse<ObjectNode> response);

	@Override
	public boolean equals(Object that) {
		return that instanceof Facet && id.equals(((Facet) that).getId());
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
