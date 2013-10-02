package com.zenobase.services;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import play.Logger;

import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskList;

public class TaskRepository {

	static final String INDEX_NAME = "tasks";

	private final Index index;

	@Inject
	public TaskRepository(IndexManager manager) {
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			Logger.info("Creating task index...");
			index.create(Integer.MAX_VALUE);
			index.putMapping(Task.getSchema());
		}
	}

	public void store(Task task, DateTime timestamp) {
		this.index.store(Task.TYPE_NAME, task.getId(), task.toJson(), timestamp, false);
	}

	public void update(Task task, DateTime timestamp) {
		index.update(Task.TYPE_NAME, task.getId(), task.toJson(), timestamp, false);
	}

	public boolean delete(String taskId) {
		return index.delete(Task.TYPE_NAME, taskId, false);
	}

	public Task find(String taskId) {
		ObjectNode node = index.get(Task.TYPE_NAME, taskId);
		return node != null ? new Task(node) : null;
	}

	public TaskList find(String field, String value, int offset, int limit) {
		return find(QueryBuilders.termQuery(field, value), offset, limit);
	}

	public TaskList find(int offset, int limit) {
		return find(QueryBuilders.matchAllQuery(), offset, limit);
	}

	private TaskList find(QueryBuilder query, int offset, int limit) {
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query).version(true).from(offset).size(limit)
			.sort(Task.CREATED.getName(), SortOrder.DESC);
		return new TaskList(index.find(search));
	}
}
