package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.commands.Command;

public abstract class TaskManager {

	public abstract String getType();

	public abstract Command configure(Task task, ObjectNode config);

	public abstract Command execute(Task task);
}
