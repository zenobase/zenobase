package com.zenobase.tasks.withings;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
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

public class WithingsSleepTaskManager extends OAuthTaskManager {

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(2);

	@Inject
	public WithingsSleepTaskManager(WithingsCredentialsManager credentialsManager) {
		super(WithingsSleepTask.TYPE, credentialsManager);
	}

	@Override
	public WithingsSleepTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "steps");
		DateTimeZone timezone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = parseMarker(settings.path("marker").textValue(), timezone);
		WithingsSleepTask task = new WithingsSleepTask(bucketId, principal, marker);
		task.setTag(tag);
		task.setTimezone(timezone);
		return task;
	}

	private static String parseMarker(String marker, DateTimeZone timezone) {
		return marker != null ? toString(LocalDateTime.parse(marker.replaceAll("Z", "")).toDateTime(timezone).withHourOfDay(12)) : null;
	}

	private static String toString(DateTime time) {
		return Long.toString(time.getMillis() / 1000);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(WithingsSleepTask.class), credentials);
	}

	private Command execute(WithingsSleepTask task, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		for (DateTime from = task.getFrom(); from.isBefore(DateTime.now()); from = from.plusWeeks(1)) {
			events.addAll(execute(task, credentials, from));
		}
		return createCommand(task, WithingsSleepResult.merge(events));
	}

	private List<Event> execute(WithingsSleepTask task, OAuthCredentials credentials, DateTime from) {
		OAuthRequest request = createRequest(task, credentials, from);
		Response response = send(request, credentials);
		WithingsSleepResult result = new WithingsSleepResult(parseObject(response), task.getPrincipal(), task.getTag(), task.useRanges(), task.getTimezone());
		Preconditions.checkState(result.getStatus() == 0, "Expected status <0> but got <%s> for task <%s>", result.getStatus(), task.getId());
		return result.getEvents();
	}

	private OAuthRequest createRequest(WithingsSleepTask task, OAuthCredentials credentials, DateTime from) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://wbsapi.withings.net/v2/sleep");
		request.addQuerystringParameter("action", "get");
		request.addQuerystringParameter("userid", credentials.getScope());
		request.addQuerystringParameter("startdate", toString(from));
		request.addQuerystringParameter("enddate", toString(from.plusWeeks(1)));
		return request;
	}

	@Override
	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		RATE_LIMITER.acquire();
		return super.send(request, credentials);
	}

	private static Command createCommand(WithingsSleepTask task, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran withings-sleep task", "reverted withings-sleep task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), !events.isEmpty() ? toString(next(events)) : task.getMarker())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}


	private static DateTime next(List<Event> events) {
		Event latest = Iterables.getLast(events);
		return Ordering.natural().max(latest.getValues(Event.TIMESTAMP));
	}
}
