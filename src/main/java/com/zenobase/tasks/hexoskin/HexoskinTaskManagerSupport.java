package com.zenobase.tasks.hexoskin;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.RateLimiter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

abstract class HexoskinTaskManagerSupport<T extends HexoskinTaskSupport> extends OAuthTaskManager {

	private static final String HOST = "https://api.hexoskin.com";
	private static final RateLimiter RATE_LIMITER = RateLimiter.create(10); // actually 400 per hour per user

	private final Class<T> taskClass;

	public HexoskinTaskManagerSupport(String type, Class<T> taskClass, HexoskinCredentialsManager credentialsManager) {
		super(type, credentialsManager);
		this.taskClass = taskClass;
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(taskClass), credentials);
	}

	private Command execute(T task, OAuthCredentials credentials) {
		HexoskinProfileResult profile = getProfile(credentials);
		List<Event> events = new ArrayList<>();
		for (String path = getPath(task); path != null; ) {
			OAuthRequest request = new OAuthRequest(Verb.GET, HOST + path);
			Response response = send(request, credentials);
			HexoskinResultSupport result = parse(response, profile, task);
			events.addAll(result.getEvents());
			path = result.next();
		}
		return createCommand(task, events);
	}

	private HexoskinProfileResult getProfile(OAuthCredentials credentials) {
		var request = new OAuthRequest(Verb.GET, "https://api.hexoskin.com/api/v1/profile/");
		Response response = send(request, credentials);
		return new HexoskinProfileResult(parseObject(response));
	}

	abstract HexoskinResultSupport parse(Response response, HexoskinProfileResult profile, T task);

	abstract String getPath(HexoskinTaskSupport task);

	@Override
	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		RATE_LIMITER.acquire();
		return super.send(request, credentials);
	}

	protected LocalDate parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker).toLocalDate() : LocalDate.now().withDayOfMonth(1);
	}

	protected Command createCommand(Task task, List<Event> events) {
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
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}

	private static @Nullable String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime time = Ordering.natural().max(event.getValues(Event.TIMESTAMP));
			if (time != null && (latest == null || time.isAfter(latest))) {
				latest = time;
			}
		}
		return latest != null ? latest.toString() : null;
	}
}
