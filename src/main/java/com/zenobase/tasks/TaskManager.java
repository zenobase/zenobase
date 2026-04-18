package com.zenobase.tasks;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.commands.Command;
import com.zenobase.models.Identity;
import org.jspecify.annotations.Nullable;

public abstract class TaskManager {

	private final String type;

	protected TaskManager(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public abstract Task newTask(String bucketId, Identity principal, ObjectNode settings);

	public abstract @Nullable Command execute(Task task);
}
