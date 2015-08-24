package com.zenobase.tasks.bodymedia;

import java.util.List;

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

public abstract class BodyMediaTaskManagerSupport extends OAuthTaskManager {

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(4);

	protected BodyMediaTaskManagerSupport(String type, BodyMediaCredentialsManager credentialsManager) {
		super(type, credentialsManager);
	}

	protected void checkRateLimit() {
		RATE_LIMITER.acquire();
	}

	protected TimezoneMap getTimezoneMap(OAuthCredentials credentials) {
		checkRateLimit();
		OAuthRequest request = new OAuthRequest(Verb.GET, String.format("http://api.bodymedia.com/v2/json/timezone"));
		Response response = send(request, credentials);
		return new BodyMediaTimezonesResult(parseObject(response)).getTimezoneMap();
	}

	protected static LocalDate parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker).toLocalDate() : LocalDate.now().withDayOfMonth(1);
	}

	protected static String formatMarker(LocalDate date) {
		return date.toString("yyyyMMdd");
	}

	protected Command createCommand(Task task, OAuthCredentials credentials, LocalDate lastSync, List<Event> events, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), lastSync.toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!credentials.getToken().equals(expiredToken)) {
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
}
