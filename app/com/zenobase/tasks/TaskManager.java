package com.zenobase.tasks;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.Command;
import com.zenobase.models.Identity;

public abstract class TaskManager {

	public abstract String getType();

	public abstract Task newTask(String bucketId, Identity principal, ObjectNode settings);

	public Command authorize(Task task, ObjectNode config) {
		throw new UnsupportedOperationException();
	}

	public void reauthorize(Task task) {
		throw new UnsupportedOperationException();
	}

	public abstract Command execute(Task task);
}
