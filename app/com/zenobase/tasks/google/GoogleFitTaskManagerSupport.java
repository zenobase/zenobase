package com.zenobase.tasks.google;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.net.UrlEscapers;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

abstract class GoogleFitTaskManagerSupport<T extends GoogleFitTaskSupport> extends OAuthTaskManager {

	private final Class<T> taskClass;

	protected GoogleFitTaskManagerSupport(String type, GoogleCredentialsManager credentialsManager, Class<T> taskClass) {
		super(type, credentialsManager);
		this.taskClass = taskClass;
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {

		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}

		Map<String, DataStream> streams = getDataStreams(credentials);
		/*for (DataStream stream : streams.values()) {
			if (stream.getId().contains("xxx")) {
				System.err.println("[" + stream.getId() + "]");
				for (DataPoint dataPoint : getDataPoints(task.as(taskClass), credentials, stream)) {
					System.err.println(dataPoint);
				}
			}
		}*/

		List<Event> events = createEvents(task.as(taskClass), credentials, streams);

		return createCommand(task, credentials, events, token);
	}

	protected abstract List<Event> createEvents(T task, OAuthCredentials credentials, Map<String, DataStream> streams);

	protected Map<String, DataStream> getDataStreams(OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://www.googleapis.com/fitness/v1/users/me/dataSources");
		Response response = send(request, credentials);
		return Maps.uniqueIndex(new DataSourcesResult(parseObject(response)).get(), new Function<DataStream, String>() {
			@Override
			public String apply(DataStream stream) {
				return stream.getId();
			}
		});
	}

	protected List<DataPoint> getDataPoints(GoogleFitTaskSupport task, OAuthCredentials credentials, DataStream stream) {
		DateTime begin = task.getFrom();
		DateTime end = DateTime.now();
		OAuthRequest request = new OAuthRequest(Verb.GET, String.format("https://www.googleapis.com/fitness/v1/users/me/dataSources/%s/datasets/%d-%d",
			UrlEscapers.urlPathSegmentEscaper().escape(stream.getId()), begin.getMillis() * 1000000, end.getMillis() * 1000000));
		Response response = send(request, credentials);
		return new DatasetResult(parseObject(response), task.getTimezone()).getDataPoints();
	}

	protected static Range<DateTime> getRange(Event event) {
		ImmutableList<DateTime> values = event.getValues(Event.TIMESTAMP);
		return Range.closed(Ordering.natural().min(values), Ordering.natural().max(values));
	}

	protected Iterable<DataStream> filter(Iterable<DataStream> streams, final String... dataTypes) {
		return Iterables.filter(streams, new Predicate<DataStream>() {
			@Override
			public boolean apply(DataStream stream) {
				for (String dataType : dataTypes) {
					if (dataType.equals(stream.getDataType())) {
						return true;
					}
				}
				return false;
			}
		});
	}

	protected Command createCommand(Task task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
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
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}

	private static String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime end = Ordering.natural().max(event.getValues(Event.TIMESTAMP));
			if (latest == null || end.isAfter(latest)) {
				latest = end.plusMillis(1);
			}
		}
		return latest != null ? latest.toString() : null;
	}
}
