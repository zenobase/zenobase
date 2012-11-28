package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.commands.Command;
import com.zenobase.models.Identity;

public abstract class TaskManager {

	public abstract String getType();

	public abstract Task newTask(String bucketId, Identity principal);

	public abstract String getConfigureUrl(Task task);

	public abstract Command configure(Task task, ObjectNode config);

	public abstract Command execute(Task task);
}
