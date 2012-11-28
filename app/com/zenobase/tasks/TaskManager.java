package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.commands.Command;
import com.zenobase.models.Identity;

public abstract class TaskManager {

	public abstract String getType();

	public abstract Task newTask(String bucketId, Identity principal);

	public String getAuthorizationUrl(Task task) {
		throw new UnsupportedOperationException();
	}

	public Command authorize(Task task, ObjectNode config) {
		throw new UnsupportedOperationException();
	}

	public abstract Command execute(Task task);
}
