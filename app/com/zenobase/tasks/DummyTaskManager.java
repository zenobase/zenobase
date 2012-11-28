package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;

public class DummyTaskManager extends TaskManager {

	@Override
	public String getType() {
		return DummyTask.TYPE;
	}

	@Override
	public String getConfigureUrl(Task task) {
		return null;
	}

	@Override
	public Command configure(Task task, ObjectNode config) {
		DummyTask to = new DummyTask(task.copy().toJson());
		to.setTag(config.get("tag").getTextValue());
		return new UpdateTaskCommand(task.getPrincipal(), task.getBucketId(), task, to);
	}

	@Override
	public Command execute(Task task) {
		return execute(new DummyTask(task.toJson()));
	}

	private Command execute(DummyTask task) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "created a dummy event", "removed a dummy event");
		Event event = new Event();
		event.setValue(Event.AUTHOR, task.getPrincipal());
		event.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC));
		event.setValue(Event.TAG, task.getTag());
		DummyTask to = task.copy();
		to.setModified(new DateTime(DateTimeZone.UTC));
		command.add(new UpdateTaskCommand(task.getPrincipal(), task.getBucketId(), task, to));
		command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		return command;
	}
}
