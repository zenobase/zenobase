package com.zenobase.tasks.rescuetime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class RescueTimeProductivityTaskManager extends OAuthTaskManager {

	private final RateLimiter rateLimit = RateLimiter.create(10);

	@Inject
	public RescueTimeProductivityTaskManager(RescueTimeCredentialsManager credentialsManager) {
		super(RescueTimeProductivityTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Strings.emptyToNull(settings.path("tag").textValue());
		String kind = Strings.emptyToNull(settings.path("kind").textValue());
		String source = Strings.emptyToNull(settings.path("source").textValue());
		DateTimeZone timezone = DateTimeZone.forID(
				MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		Task task = new RescueTimeProductivityTask(bucketId, principal, tag, kind, source, timezone);
		task.setMarker(parseMarker(settings.path("marker").textValue(), timezone));
		return task;
	}

	private static @Nullable String parseMarker(@Nullable String marker, DateTimeZone timezone) {
		return marker != null
				? LocalDateTime.parse(marker.replaceAll("Z", ""))
						.toDateTime(timezone)
						.toString()
				: null;
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(RescueTimeProductivityTask.class), credentials);
	}

	private Command execute(RescueTimeProductivityTask task, OAuthCredentials credentials) {
		DateTime last = task.getLast();
		List<Event> events = new ArrayList<>();
		for (DateTime from = last; from == null || from.isBefore(DateTime.now()); from = from.plusWeeks(1)) {
			events.addAll(get(credentials, task, from != null ? from.toLocalDate() : null));
			if (from == null) {
				from = getFirst(events);
			}
			if (from == null) {
				break;
			}
		}
		if (last != null) {
			removeNotAfter(events, last);
		}
		removeLast(events);
		return createCommand(task, events);
	}

	private List<Event> get(OAuthCredentials credentials, RescueTimeProductivityTask task, @Nullable LocalDate date) {
		rateLimit.acquire();
		OAuthRequest request = newRequest(task.getKind(), task.getSource(), date);
		Response response = send(request, credentials);
		Preconditions.checkState(
				response.getCode() == 200, "Couldn't request <%s>: %s", request.getCompleteUrl(), response.getCode());
		ObjectNode node = parseObject(response);
		var result = new ProductivityResult(node, task.getPrincipal(), task.getTag(), task.getTimezone());
		Preconditions.checkState(
				result.isSuccess(), "Request <%s> failed: %s", request.getCompleteUrl(), response.getCode());
		return result.getEvents();
	}

	private OAuthRequest newRequest(String kind, @Nullable String source, @Nullable LocalDate date) {
		var request = new OAuthRequest(Verb.GET, "https://www.rescuetime.com/api/oauth/data");
		request.addQuerystringParameter("format", "json");
		request.addQuerystringParameter("operation", "select");
		request.addQuerystringParameter("perspective", "interval");
		request.addQuerystringParameter("restrict_kind", kind);
		if (source != null) {
			request.addQuerystringParameter("restrict_source_type", source);
		}
		request.addQuerystringParameter("resolution_time", "hour");
		if (date != null) {
			request.addQuerystringParameter("restrict_begin", date.toString());
		}
		return request;
	}

	private void removeLast(List<Event> events) {
		if (!events.isEmpty()) {
			events.remove(events.size() - 1);
		}
	}

	private void removeNotAfter(List<Event> events, DateTime last) {
		events.removeIf(event ->
				!Objects.requireNonNull(event.getValue(Event.TIMESTAMP)).isAfter(last));
	}

	private Command createCommand(Task task, List<Event> events) {
		var command = new CompoundCommand(
				task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
				.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
				.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
				.set(
						Task.MARKER,
						task.getMarker(),
						events.isEmpty()
								? task.getMarker()
								: Objects.requireNonNull(getLast(events))
										.toString())
				.set(Task.UNDO, task.getUndoId(), command.getId())
				.build());
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}

	private static @Nullable DateTime getFirst(List<Event> events) {
		Event first = Iterables.getFirst(events, null);
		return first != null ? first.getValue(Event.TIMESTAMP) : null;
	}

	private static @Nullable DateTime getLast(List<Event> events) {
		Event latest = Iterables.getLast(events);
		return latest != null ? latest.getValue(Event.TIMESTAMP) : null;
	}
}
