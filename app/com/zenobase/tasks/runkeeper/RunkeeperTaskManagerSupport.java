package com.zenobase.tasks.runkeeper;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

abstract class RunkeeperTaskManagerSupport extends OAuthTaskManager {

	protected static final String host = "https://api.runkeeper.com";

	@Inject
	public RunkeeperTaskManagerSupport(String type, RunkeeperCredentialsManager credentialsManager) {
		super(type, credentialsManager);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(RunkeeperWeightTask.class), credentials);
	}

	protected static LocalDateTime parseMarker(String marker) {
		return marker != null ? LocalDateTime.parse(marker.replaceAll("Z", "")) : null;
	}

	protected static String formatMarker(LocalDateTime time) {
		return time != null ? time.toString() : null;
	}

	protected static String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime time = event.getValue(Event.TIMESTAMP);
			if (latest == null || time.isAfter(latest)) {
				latest = time;
			}
		}
		return latest != null ? latest.plusSeconds(1).toLocalDateTime().toString() : null;
	}

	protected Command createCommand(Task task, OAuthCredentials credentials, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events).toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}
}
