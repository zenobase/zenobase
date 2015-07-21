package com.zenobase.tasks.microsoft;

import java.util.List;

import javax.inject.Inject;

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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

public class MicrosoftHealthActivitiesTaskManager extends OAuthTaskManager {

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(5); // actually 500 per minute per user

	@Inject
	public MicrosoftHealthActivitiesTaskManager(MicrosoftHealthCredentialsManager credentialsManager) {
		super(MicrosoftHealthActivitiesTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		boolean metric = settings.path("metric").booleanValue();
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		return new MicrosoftHealthActivitiesTask(bucketId, principal, metric, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(MicrosoftHealthActivitiesTask.class), credentials);
	}

	private Command execute(MicrosoftHealthActivitiesTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		List<Event> events = Lists.newArrayList();
		DateTime from = parseMarker(task.getMarker());
		DateTime now = DateTime.now(DateTimeZone.UTC).minusMinutes(5);
		for (String url = "https://api.microsofthealth.net/v1/me/Activities"; url != null;) {
			OAuthRequest request = new OAuthRequest(Verb.GET, url);
			if (events.isEmpty()) {
				request.addQuerystringParameter("startTime", from.toString());
				request.addQuerystringParameter("endTime", now.toString());
				request.addQuerystringParameter("activityIncludes", "Details,MapPoints");
				request.addQuerystringParameter("maxPageSize", "100");
			}
			Response response = send(request, credentials);
			ActivitiesResult result = new ActivitiesResult(parse(response), task.getPrincipal(), task.isMetric());
			events.addAll(result.getEvents());
			url = result.next();
		}
		return createCommand(task, credentials, events, token);
	}

	static DateTime parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker) : null;
	}

	static String formatMarker(DateTime time) {
		return time != null ? time.toString() : null;
	}

	static String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime time = event.getValue(Event.TIMESTAMP);
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
