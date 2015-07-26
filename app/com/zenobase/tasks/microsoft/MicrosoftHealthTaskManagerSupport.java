package com.zenobase.tasks.microsoft;

import java.util.List;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.RateLimiter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;

abstract class MicrosoftHealthTaskManagerSupport<T extends MicrosoftHealthTaskSupport> extends OAuthTaskManager {

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(5); // actually 500 per minute per user

	private final Class<T> type;

	protected MicrosoftHealthTaskManagerSupport(Class<T> type, String typeName, MicrosoftHealthCredentialsManager credentialsManager) {
		super(typeName, credentialsManager);
		this.type = type;
	}

	@Override
	public final Command execute(Task task, OAuthCredentials credentials) {
		return executeTyped(task.as(type), credentials);
	}

	private final Command executeTyped(T task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		DateTime begin = task.getFrom();
		DateTime end = DateTime.now(task.getTimezone()).minusMinutes(5);
		List<Event> events = end.isAfter(begin) ? newEvents(task, begin, end, credentials) : ImmutableList.<Event>of();
		return createCommand(task, credentials, events, token);
	}

	protected abstract List<Event> newEvents(T task, DateTime begin, DateTime end, OAuthCredentials credentials);

	protected static DateTime markerValue(JsonNode node, DateTimeZone zone) {
		return LocalDate.parse(node.textValue()).toDateTimeAtStartOfDay(zone);
	}

	private static String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			Duration duration = event.getValue(Event.DURATION);
			DateTime time = Ordering.natural().min(event.getValues(Event.TIMESTAMP)).plus(duration);
			if (latest == null || time.isAfter(latest)) {
				latest = time;
			}
		}
		return latest != null ? latest.toString() : null;
	}

	private Command createCommand(Task task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events).toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!Objects.equal(credentials.getToken(), expiredToken)) {
			command.add(UpdateCredentialsCommand.builder(credentials)
				.with(Credentials.CREDENTIALS)
				.set(OAuthCredentials.TOKEN, expiredToken, credentials.getToken())
				.build());
		}
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}

	@Override
	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		RATE_LIMITER.acquire();
		return super.send(request, credentials);
	}
}
