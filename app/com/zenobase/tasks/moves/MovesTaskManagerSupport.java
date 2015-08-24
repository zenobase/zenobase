package com.zenobase.tasks.moves;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.RateLimiter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

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

abstract class MovesTaskManagerSupport extends OAuthTaskManager {

	private static final String BASE_URL = "https://api.moves-app.com/api/1.1";
	private static final RateLimiter RATE_LIMITER = RateLimiter.create(1);

	protected MovesTaskManagerSupport(String type, MovesCredentialsManager credentialsManager) {
		super(type, credentialsManager);
	}

	@Override
	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		RATE_LIMITER.acquire();
		return super.send(request, credentials);
	}

	protected MovesProfileResult getProfile(OAuthCredentials credentials) {
		OAuthRequest request = newRequest("/user/profile");
		Response response = send(request, credentials);
		return new MovesProfileResult(parseObject(response));
	}

	protected void removeDuplicates(List<Event> events) {
		DateTime t0 = null;
		for (Iterator<Event> i = events.iterator(); i.hasNext();) {
			Event event = i.next();
			DateTime t1 = event.getValue(Event.TIMESTAMP);
			if (t0 == null || !t0.equals(t1)) {
				t0 = t1;
			} else {
				i.remove();
			}
		}
	}

	protected void removeLast(List<Event> events) {
		if (!events.isEmpty()) {
			events.remove(events.size() - 1);
		}
	}

	protected Command createCommand(Task task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
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

	protected static String getMarker(List<Event> events) {
		Event latest = Iterables.getLast(events);
		return latest.getValue(Event.TIMESTAMP).plus(latest.getValue(Event.DURATION)).toString();
	}

	protected static LocalDate min(LocalDate a, LocalDate b) {
		return a.isAfter(b) ? b : a;
	}

	protected static OAuthRequest newRequest(String path) {
		return new OAuthRequest(Verb.GET, BASE_URL + path);
	}
}
