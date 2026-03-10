package com.zenobase.services;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.joda.time.DateTime;
import play.Logger;

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskList;

public class TaskRepository extends RepositorySupport<Task> {

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

	public PartialList<Task> find(int offset, int limit) {
		return find(new TaskQuery(), offset, limit);
	}

	public PartialList<Task> find(TaskQuery query, int offset, int limit) {
		return find(query, TaskQuery.orderByCreated(false), offset, limit);
	}

	public PartialList<Task> find(TaskQuery query, SearchOrder order, int offset, int limit) {
		SearchRequest.Builder builder = new SearchRequest.Builder()
			.index(index.getIndexName())
			.query(query.build()).version(true).seqNoPrimaryTerm(true)
			.from(offset).size(limit);
		order.apply(builder);
		return new TaskList(index.find(builder.build()));
	}

	public void find(TaskQuery query, Callback<Task> callback) {
		super.find(query.build(), callback);
	}

	public void refresh() {
		index.refresh();
	}

	@Override
	protected Index getIndex() {
		return index;
	}

	@Override
	protected Task toObject(ObjectNode node) {
		return new Task(node);
	}
}
