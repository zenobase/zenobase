package com.zenobase.tasks.google;

import com.google.common.collect.*;
import com.google.common.net.UrlEscapers;
import com.zenobase.commands.*;
import com.zenobase.models.Event;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

abstract class GoogleFitTaskManagerSupport<T extends GoogleFitTaskSupport> extends OAuthTaskManager {

	private final Class<T> taskClass;

	protected GoogleFitTaskManagerSupport(
		String type,
		GoogleCredentialsManager credentialsManager,
		Class<T> taskClass
	) {
		super(type, credentialsManager);
		this.taskClass = taskClass;
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		Token token = Objects.requireNonNull(credentials.getToken());
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}

		ImmutableMap<String, DataStream> streams = getDataStreams(credentials);

		return execute(task.as(taskClass), streams, credentials, token);
	}

	protected Command execute(T task, Map<String, DataStream> streams, OAuthCredentials credentials, Token token) {
		List<Event> events = createEvents(task, credentials, streams);
		return createCommand(task, credentials, events, token);
	}

	protected List<Event> createEvents(T task, OAuthCredentials credentials, Map<String, DataStream> streams) {
		throw new UnsupportedOperationException();
	}

	private ImmutableMap<String, DataStream> getDataStreams(OAuthCredentials credentials) {
		var request = new OAuthRequest(Verb.GET, "https://www.googleapis.com/fitness/v1/users/me/dataSources");
		Response response = send(request, credentials);
		return Maps.uniqueIndex(new DataSourcesResult(parseObject(response)).get(), DataStream::id);
	}

	protected void getDataPoints(
		GoogleFitTaskSupport task,
		OAuthCredentials credentials,
		DataStream stream,
		Consumer<DataPoint> consumer
	) {
		getDataPoints(
			Objects.requireNonNull(task.getFrom()),
			DateTime.now(),
			task.getTimezone(),
			credentials,
			stream,
			consumer
		);
	}

	private void getDataPoints(
		DateTime begin,
		DateTime end,
		DateTimeZone zone,
		OAuthCredentials credentials,
		DataStream stream,
		Consumer<DataPoint> consumer
	) {
		String pageToken = null;
		do {
			var request = new OAuthRequest(
				Verb.GET,
				String.format(
					"https://www.googleapis.com/fitness/v1/users/me/dataSources/%s/datasets/%d-%d",
					UrlEscapers.urlPathSegmentEscaper().escape(stream.id()),
					begin.getMillis() * 1000000,
					end.getMillis() * 1000000
				)
			);
			request.addQuerystringParameter("limit", "1000");
			if (pageToken != null) {
				request.addQuerystringParameter("pageToken", pageToken);
			}
			Response response = send(request, credentials);
			DatasetResult result = new DatasetResult(parseObject(response), zone);
			result.getDataPoints().forEach(consumer);
			pageToken = result.getNextPageToken();
		} while (pageToken != null);
	}

	protected static Range<DateTime> getRange(Event event) {
		ImmutableList<DateTime> values = event.getValues(Event.TIMESTAMP);
		return Range.closed(
			Objects.requireNonNull(Ordering.natural().min(values)),
			Objects.requireNonNull(Ordering.natural().max(values))
		);
	}

	protected Iterable<DataStream> filter(Iterable<DataStream> streams, String... dataTypes) {
		return Iterables.filter(streams, stream -> {
			for (String dataType : dataTypes) {
				if (dataType.equals(stream.dataType())) {
					return true;
				}
			}
			return false;
		});
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
			DateTime end = Ordering.natural().max(event.getValues(Event.TIMESTAMP));
			if (end != null && (latest == null || end.isAfter(latest))) {
				latest = end.plusMillis(1);
			}
		}
		return latest != null ? latest.toString() : null;
	}
}
