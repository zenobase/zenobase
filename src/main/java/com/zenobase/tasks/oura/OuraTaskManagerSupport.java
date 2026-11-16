package com.zenobase.tasks.oura;

import com.google.common.collect.Ordering;
import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;
import org.scribe.model.Token;

abstract class OuraTaskManagerSupport extends OAuthTaskManager {

	protected static final String HOST = "https://api.ouraring.com";

	protected OuraTaskManagerSupport(String type, OuraCredentialsManager credentialsManager) {
		super(type, credentialsManager);
	}

	/**
	 * Oura returns whole days starting at the marker's date, so drop the records that were imported by an earlier run.
	 */
	protected Command createCommand(Task task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
		DateTime marker = DateTime.parse(Objects.requireNonNull(task.getMarker()));
		events.removeIf(event -> !getEnd(event).isAfter(marker));
		var command = new CompoundCommand(
			task.getPrincipal(),
			"ran " + getType() + " task",
			"reverted " + getType() + " task"
		);
		command.add(
			UpdateTaskCommand.builder(task)
				.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
				.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
				.set(Task.MARKER, task.getMarker(), task.laterMarker(marker, getLatest(events)))
				.set(Task.UNDO, task.getUndoId(), command.getId())
				.build()
		);
		if (!Objects.equals(credentials.getToken(), expiredToken)) {
			command.add(
				UpdateCredentialsCommand.builder(credentials)
					.with(Credentials.CREDENTIALS)
					.set(OAuthCredentials.TOKEN, expiredToken, credentials.getToken())
					.build()
			);
		}
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}

	private static @Nullable DateTime getLatest(List<Event> events) {
		return events.stream().map(OuraTaskManagerSupport::getEnd).max(Comparator.naturalOrder()).orElse(null);
	}

	private static DateTime getEnd(Event event) {
		return Objects.requireNonNull(Ordering.natural().max(event.getValues(Event.TIMESTAMP)));
	}
}
