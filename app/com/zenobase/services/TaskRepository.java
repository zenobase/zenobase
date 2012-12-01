package com.zenobase.services;

import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import play.Logger;
import com.google.common.collect.Lists;

import com.zenobase.common.PartialList;
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

	public void store(Task task) {
		this.index.store(Task.TYPE_NAME, task.getId(), task.toJson(), false);
	}

	public void update(Task task) {
		index.update(Task.TYPE_NAME, task.getId(), task.toJson(), false);
	}

	public boolean delete(String taskId) {
		return index.delete(Task.TYPE_NAME, taskId, false);
	}

	public Task findTask(String taskId) {
		ObjectNode node = index.get(Task.TYPE_NAME, taskId);
		return node != null ? new Task(node) : null;
	}

	public TaskList findTasks(String field, String value, int offset, int limit) {
		Logger.info(String.format("find(%s, %s)", field, value));
		return findTasks(queryFor(field, value), offset, limit);
	}

	private static QueryBuilder queryFor(String field, String value) {
		return value == null
			? QueryBuilders.matchAllQuery()
			: QueryBuilders.termQuery(field, value);
	}

	private TaskList findTasks(QueryBuilder query, int offset, int limit) {
		List<Task> tasks = Lists.newArrayListWithCapacity(limit);
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query).version(true).from(offset).size(limit)
			.sort(Task.CREATED.getName(), SortOrder.DESC);
		PartialList<ObjectNode> hits = index.find(search);
		for (ObjectNode hit : hits.getElements()) {
			tasks.add(new Task(hit));
		}
		return new TaskList(tasks, hits.size());
	}
}
