package com.zenobase.services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.joda.time.DateTime;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskList;

public class TaskRepository extends RepositorySupport<Task> {

	private static final Logger logger = LoggerFactory.getLogger(TaskRepository.class);

	static final String INDEX_NAME = "tasks";

	private final Index index;

	@Inject
	public TaskRepository(IndexManager manager) {
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			logger.info("Creating task index...");
			index.create(Integer.MAX_VALUE);
			index.putMapping(Task.getSchema());
		}
	}

	public void store(Task task, DateTime timestamp) {
		this.index.store(task, false);
	}

	public void update(Task task, DateTime timestamp) {
		index.update(task, false);
	}

	public boolean delete(String taskId) {
		return index.delete(taskId, false);
	}

	public @Nullable Task find(String taskId) {
		ObjectNode node = index.get(taskId);
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
				.query(query.build())
				.version(true)
				.seqNoPrimaryTerm(true)
				.from(offset)
				.size(limit)
				.trackTotalHits(t -> t.enabled(true));
		order.apply(builder);
		return new TaskList(index.find(builder.build()));
	}

	public void find(TaskQuery query, Callback<Task> callback) {
		super.find(query.build(), callback);
	}

	@Override
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
