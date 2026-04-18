package com.zenobase.tasks.wakatime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.RateLimiter;
import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

public class WakaTimeTaskManager extends OAuthTaskManager {

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(5);
	private static final String HOST = "https://wakatime.com/api/v1";

	@Inject
	public WakaTimeTaskManager(WakaTimeCredentialsManager credentialsManager) {
		super(WakaTimeTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Strings.emptyToNull(settings.path("tag").textValue());
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new WakaTimeTask(bucketId, principal, tag, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(WakaTimeTask.class), credentials);
	}

	private Command execute(WakaTimeTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		DateTime begin = Objects.requireNonNull(task.getBegin());
		LocalDate today = LocalDate.now(DateTimeZone.UTC).plusDays(1);
		List<Event> events = new ArrayList<>();
		for (LocalDate date = begin.toLocalDate(); !date.isAfter(today); date = date.plusDays(1)) {
			RATE_LIMITER.acquire();
			var request = new OAuthRequest(Verb.GET, HOST + "/users/current/durations");
			request.addQuerystringParameter("date", date.toString());
			try {
				Response response = send(request, credentials);
				var result = new WakaTimeDurationsResult(parseObject(response), task.getPrincipal(), task.getTag());
				for (Event event : result.getEvents()) {
					if (Objects.requireNonNull(event.getValue(Event.TIMESTAMP)).isAfter(begin)) {
						events.add(event);
					}
				}
			} catch (InvalidStatusException e) {
				if (e.getStatus() == 402 && date.isBefore(today.minusWeeks(2))) {
					// free WakaTime accounts are limited to 2 weeks of history
					date = today.minusWeeks(2);
				} else {
					throw e;
				}
			}
		}
		return createCommand(task, credentials, events, Objects.requireNonNull(token));
	}

	@Override
	protected boolean isSuccessful(Response response) {
		return response.isSuccessful() || response.getCode() == 400; // 400 = no data available
	}

	protected Command createCommand(Task task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
		var command = new CompoundCommand(
			task.getPrincipal(),
			"ran " + getType() + " task",
			"reverted " + getType() + " task"
		);
		command.add(
			UpdateTaskCommand.builder(task)
				.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
				.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
				.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events))
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

	private static @Nullable String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime time = Objects.requireNonNull(Ordering.natural().max(event.getValues(Event.TIMESTAMP)));
			if (latest == null || time.isAfter(latest)) {
				latest = time;
			}
		}
		return latest != null ? latest.toString() : null;
	}
}
