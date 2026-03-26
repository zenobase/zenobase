package com.zenobase.tasks.fitbark;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Ordering;
import jakarta.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class FitBarkTaskManager extends OAuthTaskManager {

	private static final Logger logger = LoggerFactory.getLogger(FitBarkTaskManager.class);

	@Inject
	public FitBarkTaskManager(FitBarkCredentialsManager credentialsManager) {
		super(FitBarkTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String name = Preconditions.checkNotNull(settings.path("name").textValue());
		boolean hourly = settings.path("hourly").booleanValue();
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		return new FitBarkTask(bucketId, principal, name, hourly, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(FitBarkTask.class), credentials);
	}

	private Command execute(FitBarkTask task, OAuthCredentials credentials) {
		List<Event> events = new ArrayList<>();
		Dog dog = findDog(Objects.requireNonNull(task.getName()), credentials);
		if (dog != null) {
			DateTime marker =
					Objects.requireNonNull(Ordering.natural().max(parseMarker(task.getMarker()), dog.getCreated()));
			LocalDate from = marker.toLocalDate();
			while (!from.isAfter(dog.getModified().toLocalDate())) {
				LocalDate to = task.isHourly() ? from.plusDays(7) : from.plusMonths(1);
				ObjectNode payload = Nodes.newObject();
				payload.putObject("activity_series")
						.put("slug", dog.getId())
						.put("resolution", task.isHourly() ? "HOURLY" : "DAILY")
						.put("from", from.toString())
						.put("to", to.toString());
				var request = new OAuthRequest(Verb.POST, "https://app.fitbark.com/api/v2/activity_series");
				request.addHeader("Content-Type", "application/json");
				request.addPayload(payload.toString());
				Response response = send(request, credentials);
				events.addAll(new ActivitySeriesResult(
								dog.getName(),
								task.getPrincipal(),
								marker,
								dog.getModified().getZone(),
								parseObject(response))
						.getEvents());
				from = to;
			}
		} else {
			logger.warn("Dog not found: {}", task.getName());
		}
		if (!events.isEmpty()) {
			events.remove(events.size() - 1); // the most recent record could be incomplete
		}
		return createCommand(task, events);
	}

	private @Nullable Dog findDog(String name, OAuthCredentials credentials) {
		var request = new OAuthRequest(Verb.GET, "https://app.fitbark.com/api/v2/dog_relations");
		Response response = send(request, credentials);
		for (Dog dog : new DogsResult(parse(response)).getDogs()) {
			if (dog.getName().equalsIgnoreCase(name)) {
				return dog;
			}
		}
		return null;
	}

	static @Nullable DateTime parseMarker(@Nullable String marker) {
		return marker != null ? DateTime.parse(marker) : null;
	}

	static @Nullable String formatMarker(@Nullable DateTime time) {
		return time != null ? time.toString() : null;
	}

	static @Nullable String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime time = Objects.requireNonNull(event.getValue(Event.TIMESTAMP));
			if (latest == null || time.isAfter(latest)) {
				latest = time;
			}
		}
		return latest != null ? latest.toString() : null;
	}

	private Command createCommand(Task task, List<Event> events) {
		var command = new CompoundCommand(
				task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
				.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
				.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
				.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events))
				.set(Task.UNDO, task.getUndoId(), command.getId())
				.build());
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}
}
