package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.DomainNode;
import com.zenobase.tasks.Task;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public class CreateTaskForm extends DomainNode {

	public CreateTaskForm(ObjectNode node) {
		super(node);
	}

	public CreateTaskForm(String bucketId, String type) {
		setValue(Task.TYPE, type);
		setValue(Task.BUCKET, bucketId);
	}

	public String getType() {
		return Objects.requireNonNull(getValue(Task.TYPE));
	}

	public String getBucketId() {
		return Objects.requireNonNull(getValue(Task.BUCKET));
	}

	public @Nullable ObjectNode getSettings() {
		return getValue(Task.SETTINGS);
	}

	public boolean valid() {
		return getValue(Task.TYPE) != null && getValue(Task.BUCKET) != null;
	}
}
