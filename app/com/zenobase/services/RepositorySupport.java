package com.zenobase.services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import com.zenobase.common.Callback;

public abstract class RepositorySupport<T> {

	public void disableRefresh(boolean disable) {
		getIndex().disableRefresh(disable);
	}

	public void refresh() {
		getIndex().refresh();
	}

	public void findAll(Callback<T> callback) {
		find(Query.of(q -> q.matchAll(m -> m)), callback);
	}

	protected void find(Query query, Callback<T> callback) {
		getIndex().find(query, node -> callback.call(toObject(node)), 1000);
	}

	protected abstract Index getIndex();

	protected abstract T toObject(ObjectNode node);
}
