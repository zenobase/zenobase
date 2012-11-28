package com.zenobase.controllers;

import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.json.DomainNode;
import com.zenobase.tasks.Task;

public class CreateTaskForm extends DomainNode {

	public CreateTaskForm(ObjectNode node) {
		super(node);
	}

	public String getType() {
		return getValue(Task.TYPE);
	}

	public String getBucketId() {
		return getValue(Task.BUCKET);
	}

	public boolean valid() {
		return getType() != null && getBucketId() != null;
	}
}
