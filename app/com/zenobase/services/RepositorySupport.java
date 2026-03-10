package com.zenobase.services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

import com.zenobase.common.Callback;

public abstract class RepositorySupport<T> {

	public void findAll(Callback<T> callback) {
		find(QueryBuilders.matchAllQuery(), callback);
	}

	protected void find(QueryBuilder query, Callback<T> callback) {
		getIndex().find(query, node -> callback.call(toObject(node)), 100);
	}

	protected abstract Index getIndex();

	protected abstract T toObject(ObjectNode node);
}
