package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
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
import java.util.List;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

/**
 * Shared plumbing for Google Health API tasks. The Google Health REST API (v4) exposes per-data-type resources at
 * {@code https://health.googleapis.com/v4/users/me/{dataType}} with {@code :list} / {@code :dailyRollup} /
 * {@code :rollUp} methods. Subclasses wire up the data-type specific URL path and result parser; this class handles
 * token refresh, pagination, and emitting the {@link CompoundCommand} that advances the marker and stores events.
 */
abstract class GoogleHealthTaskManagerSupport<T extends GoogleHealthTaskSupport> extends OAuthTaskManager {

	protected static final String BASE_URL = "https://health.googleapis.com/v4/users/me/";

	private final Class<T> taskClass;

	protected GoogleHealthTaskManagerSupport(
		String type,
		GoogleHealthCredentialsManager credentialsManager,
		Class<T> taskClass
	) {
		super(type, credentialsManager);
		this.taskClass = taskClass;
	}

	@Override
	public @Nullable Command execute(Task task, OAuthCredentials credentials) {
		Token token = Objects.requireNonNull(credentials.getToken());
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		T typed = task.as(taskClass);
		List<Event> events = createEvents(typed, credentials);
		return createCommand(typed, credentials, events, token);
	}

	/**
	 * Fetch and parse events for this task. Implementations paginate via {@code pageToken} and advance
	 * {@link #rangeFor(GoogleHealthTaskSupport)} as needed.
	 */
	protected abstract List<Event> createEvents(T task, OAuthCredentials credentials);

	/**
	 * Issue one or more paginated GET requests under a given resource path. {@code extractor} is called once per page
	 * with the parsed response body and must return the page's {@code nextPageToken} (or null when finished).
	 */
	protected void paginate(
		OAuthCredentials credentials,
		String resource,
		DateTime startTime,
		DateTime endTime,
		PageHandler handler
	) {
		String pageToken = null;
		do {
			var request = new OAuthRequest(Verb.GET, BASE_URL + resource);
			request.addQuerystringParameter("startTime", startTime.toString());
			request.addQuerystringParameter("endTime", endTime.toString());
			if (pageToken != null) {
				request.addQuerystringParameter("pageToken", pageToken);
			}
			Response response = send(request, credentials);
			pageToken = handler.handle(parseObject(response));
		} while (pageToken != null && !pageToken.isEmpty());
	}

	protected static TimeRange rangeFor(GoogleHealthTaskSupport task) {
		DateTime from = Objects.requireNonNull(task.getFrom(), "task has no marker");
		return new TimeRange(from, DateTime.now(DateTimeZone.UTC));
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
			ImmutableList<DateTime> timestamps = event.getValues(Event.TIMESTAMP);
			if (timestamps.isEmpty()) {
				continue;
			}
			DateTime end = Objects.requireNonNull(Ordering.natural().max(timestamps));
			if (latest == null || end.isAfter(latest)) {
				latest = end.plusMillis(1);
			}
		}
		return latest != null ? latest.toString() : null;
	}

	@FunctionalInterface
	protected interface PageHandler {
		/** Handle one page of data. Return the {@code nextPageToken} (null/empty when finished). */
		@Nullable
		String handle(ObjectNode page);
	}

	protected record TimeRange(DateTime startTime, DateTime endTime) {}
}
