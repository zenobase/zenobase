package com.zenobase.tasks;

import com.zenobase.commands.Command;

public abstract class TaskManager<T extends Task> {

	public abstract Command execute(T task);
}
