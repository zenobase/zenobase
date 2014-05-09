package com.zenobase.tasks.rescuetime;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
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
		DateTimeZone timezone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		Task task = new RescueTimeProductivityTask(bucketId, principal, tag, kind, timezone);
		task.setMarker(parseMarker(settings.path("marker").textValue(), timezone));
		return task;
	}

	private static String parseMarker(String marker, DateTimeZone timezone) {
		return marker != null ? LocalDateTime.parse(marker.replaceAll("Z", "")).toDateTime(timezone).toString() : null;
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(RescueTimeProductivityTask.class), credentials);
	}

	private Command execute(RescueTimeProductivityTask task, OAuthCredentials credentials) {
		DateTime last = task.getLast();
		List<Event> events = Lists.newArrayList();
		for (DateTime from = last; from == null || from.isBefore(DateTime.now()); from = from.plusWeeks(1)) {
			events.addAll(get(credentials, task.getTag(), task.getKind(), task.getTimezone(), from != null ? from.toLocalDate() : null));
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

	private List<Event> get(OAuthCredentials credentials, String tag, String kind, DateTimeZone timezone, LocalDate date) {
		rateLimit.acquire();
		OAuthRequest request = newRequest(kind, date);
		Response response = send(request, credentials);
		Preconditions.checkState(response.getCode() == 200,
			"Couldn't request <%s>: %s", request.getCompleteUrl(), response.getBody());
		ObjectNode node = parseObject(response);
		ProductivityResult result = new ProductivityResult(node, tag, timezone);
		Preconditions.checkState(result.isSuccess(),
			"Request <%s> failed: %s", request.getCompleteUrl(), response.getBody());
		return result.getEvents();
	}

	private OAuthRequest newRequest(String kind, LocalDate date) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://www.rescuetime.com/api/oauth/data");
		request.addQuerystringParameter("format", "json");
		request.addQuerystringParameter("operation", "select");
		request.addQuerystringParameter("perspective", "interval");
		request.addQuerystringParameter("restrict_kind", kind);
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
		for (Iterator<Event> i = events.iterator(); i.hasNext();) {
			if (!i.next().getValue(Event.TIMESTAMP).isAfter(last)) {
				i.remove();
			}
		}
	}

	private Command createCommand(Task task, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getLast(events).toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}

	private static DateTime getFirst(List<Event> events) {
		Event first = Iterables.getFirst(events, null);
		return first != null ? first.getValue(Event.TIMESTAMP) : null;
	}

	private static DateTime getLast(List<Event> events) {
		Event latest = Iterables.getLast(events);
		return latest.getValue(Event.TIMESTAMP);
	}
}
