package com.zenobase.tasks.nokia;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
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
import com.zenobase.tasks.Task;

public class NokiaHealthSleepTaskManager extends NokiaHealthTaskManagerSupport<NokiaHealthSleepTask> {

	@Inject
	public NokiaHealthSleepTaskManager(NokiaHealthCredentialsManager credentialsManager) {
		super(NokiaHealthSleepTask.TYPE, NokiaHealthSleepTask.class, credentialsManager);
	}

	@Override
	public NokiaHealthSleepTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "steps");
		DateTimeZone timezone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = parseMarker(settings.path("marker").textValue(), timezone);
		NokiaHealthSleepTask task = new NokiaHealthSleepTask(bucketId, principal, marker);
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
	Command safeExecute(NokiaHealthSleepTask task, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		for (DateTime from = task.getFrom(); from.isBefore(DateTime.now()); from = from.plusWeeks(1)) {
			events.addAll(execute(task, credentials, from));
		}
		return createCommand(task, NokiaHealthSleepResult.merge(events));
	}

	private List<Event> execute(NokiaHealthSleepTask task, OAuthCredentials credentials, DateTime from) {
		OAuthRequest request = createRequest(from);
		Response response = send(request, credentials);
		NokiaHealthSleepResult result = new NokiaHealthSleepResult(parseObject(response), task.getPrincipal(), task.getTag(), task.useRanges(), task.getTimezone());
		Preconditions.checkState(result.getStatus() == 0, "Expected status <0> but got <%s> for task <%s>", result.getStatus(), task.getId());
		return result.getEvents();
	}

	private OAuthRequest createRequest(DateTime from) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://wbsapi.withings.net/v2/sleep");
		request.addQuerystringParameter("action", "get");
		request.addQuerystringParameter("startdate", toString(from));
		request.addQuerystringParameter("enddate", toString(from.plusWeeks(1)));
		return request;
	}

	private static Command createCommand(NokiaHealthSleepTask task, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran nokia-sleep task", "reverted nokia-sleep task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), !events.isEmpty() ? toString(next(events)) : task.getMarker())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}

	private static DateTime next(List<Event> events) {
		Event latest = Iterables.getLast(events);
		return Ordering.natural().max(latest.getValues(Event.TIMESTAMP));
	}
}
