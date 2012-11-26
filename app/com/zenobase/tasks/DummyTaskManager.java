package com.zenobase.tasks;

import com.zenobase.commands.Command;

public class DummyTaskManager extends TaskManager<DummyTask> {

	@Override
	public Command execute(DummyTask task) {
		return null; // TODO
	}
}
