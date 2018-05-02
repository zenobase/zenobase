package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.DomainNode;
import com.zenobase.tasks.Task;

public class CreateTaskForm extends DomainNode {

	public CreateTaskForm(ObjectNode node) {
		super(node);
	}

	public CreateTaskForm(String bucketId, String type) {
		setValue(Task.TYPE, type);
		setValue(Task.BUCKET, bucketId);
	}

	public String getType() {
		return getValue(Task.TYPE);
	}

	public String getBucketId() {
		return getValue(Task.BUCKET);
	}

	public ObjectNode getSettings() {
		return getValue(Task.SETTINGS);
	}

	public boolean valid() {
		return getType() != null && getBucketId() != null;
	}
}
