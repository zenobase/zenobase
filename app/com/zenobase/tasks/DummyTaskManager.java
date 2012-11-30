package com.zenobase.tasks;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

public class DummyTaskManager extends TaskManager {

	private static final Resource SOURCE = new Resource("Zenobase", "http://zenobase.com/");

	@Override
	public String getType() {
		return DummyTask.TYPE;
	}

	@Override
	public Task newTask(String bucketId, Identity principal) {
		DummyTask task = new DummyTask(bucketId, principal, null);
		task.setEnabled(true);
		task.setTag("test"); // TODO read from settings
		return task;
	}

	@Override
	public Command execute(Task task) {
		return execute(task.as(DummyTask.class));
	}

	private Command execute(DummyTask task) {
		Preconditions.checkState(task.isEnabled(), "Task is not enabled: %s", task.getId());
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "created a dummy event", "removed a dummy event");
		Event event = new Event();
		event.setValue(Event.AUTHOR, task.getPrincipal());
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC));
		event.setValue(Event.TAG, task.getTag());
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.build());
		command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		return command;
	}
}
