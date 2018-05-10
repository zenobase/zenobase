package com.zenobase.tasks.wakatime;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.RateLimiter;
import org.elasticsearch.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import play.mvc.Http;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

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
		DateTime begin = task.getBegin();
		LocalDate today = LocalDate.now(DateTimeZone.UTC).plusDays(1);
		List<Event> events = Lists.newArrayList();
		for (LocalDate date = begin.toLocalDate(); !date.isAfter(today); date = date.plusDays(1)) {
			RATE_LIMITER.acquire();
			OAuthRequest request = new OAuthRequest(Verb.GET, HOST + "/users/current/durations");
			request.addQuerystringParameter("date", date.toString());
			Response response = send(request, credentials);
			WakaTimeDurationsResult result = new WakaTimeDurationsResult(parseObject(response), task.getPrincipal(), task.getTag());
			for (Event event : result.getEvents()) {
				if (event.getValue(Event.TIMESTAMP).isAfter(begin)) {
					events.add(event);
				}
			}
		}
		return createCommand(task, credentials, events, token);
	}

	@Override
	protected boolean isSuccessful(Response response) {
		return response.isSuccessful() || response.getCode() == Http.Status.BAD_REQUEST; // 400 = no data available
	}

	protected Command createCommand(Task task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events))
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

	private static String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime time = Ordering.natural().max(event.getValues(Event.TIMESTAMP));
			if (latest == null || time.isAfter(latest)) {
				latest = time;
			}
		}
		return latest != null ? latest.toString() : null;
	}
}
