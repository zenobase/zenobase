package com.zenobase.services;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Callback;

public abstract class RepositorySupport<T> {

	public void findAll(final Callback<T> callback) {
		find(QueryBuilders.matchAllQuery(), callback);
	}

	protected void find(QueryBuilder query, final Callback<T> callback) {
		getIndex().find(query, new Callback<ObjectNode>() {
			@Override
			public void call(ObjectNode node) {
				callback.call(toObject(node));
			}
		}, 100);
	}

	protected abstract Index getIndex();

	protected abstract T toObject(ObjectNode node);
}
