package com.zenobase.tasks.demo;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.util.concurrent.Uninterruptibles;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManager;

public class DemoTaskManager extends TaskManager {

	private static final Resource SOURCE = new Resource("Zenobase", "http://zenobase.com/");

	public DemoTaskManager() {
		super(DemoTask.TYPE);
	}

	@Override
	public DemoTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "test");
		return new DemoTask(bucketId, principal, tag);
	}

	@Override
	public Command execute(Task task) {
		Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
		return execute(task.as(DemoTask.class));
	}

	private Command execute(DemoTask task) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran demo task", "reverted demo task");
		Event event = new Event();
		event.setValue(Event.AUTHOR, task.getPrincipal());
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC));
		event.setValue(Event.TAG, task.getTag());
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		return command;
	}
}
