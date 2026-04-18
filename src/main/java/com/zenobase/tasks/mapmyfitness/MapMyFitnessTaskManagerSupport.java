package com.zenobase.tasks.mapmyfitness;

import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.RateLimiter;
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
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

abstract class MapMyFitnessTaskManagerSupport extends OAuthTaskManager {

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(10);
	protected static final String HOST = "https://oauth2-api.mapmyapi.com";

	@Inject
	public MapMyFitnessTaskManagerSupport(String type, MapMyFitnessCredentialsManager credentialsManager) {
		super(type, credentialsManager);
	}

	protected UserResult getUser(OAuthCredentials credentials) {
		var request = new OAuthRequest(Verb.GET, HOST + "/v7.0/user/self/");
		Response response = send(request, credentials);
		return new UserResult(parseObject(response));
	}

	static @Nullable String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime time = Objects.requireNonNull(Ordering.natural().max(event.getValues(Event.TIMESTAMP)));
			if (latest == null || time.isAfter(latest)) {
				latest = time;
			}
		}
		return latest != null ? latest.plusSeconds(1).toString() : null;
	}

	@Override
	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		RATE_LIMITER.acquire();
		return super.send(request, credentials);
	}

	protected Command createCommand(
		Task task,
		OAuthCredentials credentials,
		List<Event> events,
		@Nullable Token expiredToken
	) {
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
}
