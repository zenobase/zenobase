package com.zenobase.tasks.google;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import org.elasticsearch.common.collect.Ordering;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.common.net.UrlEscapers;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class GoogleFitTaskManager extends OAuthTaskManager {

	private static final Resource DEFAULT_SOURCE = new Resource("Google Fit", "https://fit.google.com/");

	@Inject
	public GoogleFitTaskManager(GoogleCredentialsManager credentialsManager) {
		super(GoogleFitTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		DateTime begin = DateTime.parse(settings.path("marker").textValue()).withZoneRetainFields(zone);
		boolean metric = settings.path("metric").booleanValue();
		boolean derived = settings.path("derived").booleanValue();
		return new GoogleFitTask(bucketId, principal, zone, metric, derived, begin.toString());
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(GoogleFitTask.class), credentials);
	}

	private Command execute(GoogleFitTask task, OAuthCredentials credentials) {

		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}

		Map<String, DataStream> streams = getDataStreams(credentials);
		/*for (DataStream stream : streams.values()) {
			if (stream.getId().contains("xxx")) {
				System.err.println("[" + stream.getId() + "]");
				for (DataPoint dataPoint : getDataPoints(task, credentials, stream)) {
					System.err.println(dataPoint);
				}
			}
		}*/

		List<Event> events = task.useDerived()
			? createEventsFromActivities(task, credentials, streams)
			: createEventsFromSessions(task, credentials);

		if (!events.isEmpty()) {
			addLocation(task, credentials, streams.get("derived:com.google.location.sample:com.google.android.gms:merge_location_samples"), events);
			addDistance(task, credentials, streams.get("derived:com.google.distance.delta:com.google.android.gms:pruned_distance"), events);
			addCount(task, credentials, streams.get("derived:com.google.step_count.delta:com.google.android.gms:merge_step_deltas"), events);
			addVelocity(task, credentials, filter(streams.values(), "com.google.speed.summary"), events);
			addEnergy(task, credentials, filter(streams.values(), "com.google.calories.expended"), events);
			addFrequency(task, credentials, filter(streams.values(), "com.google.heart_rate.summary"), events);
			setDefaultSource(events);
		}

		return createCommand(task, credentials, events, token);
	}

	private Map<String, DataStream> getDataStreams(OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://www.googleapis.com/fitness/v1/users/me/dataSources");
		Response response = send(request, credentials);
		return Maps.uniqueIndex(new DataSourcesResult(parseObject(response)).get(), new Function<DataStream, String>() {
			@Override
			public String apply(DataStream stream) {
				return stream.getId();
			}
		});
	}

	private List<Event> createEventsFromSessions(GoogleFitTask task, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		String pageToken = null;
		do {
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://www.googleapis.com/fitness/v1/users/me/sessions");
			DateTime from = task.getFrom();
			if (from != null) {
				request.addQuerystringParameter("startTime", from.toString());
			}
			if (pageToken != null) {
				request.addQuerystringParameter("pageToken", pageToken);
			}
			Response response = send(request, credentials);
			SessionsResult result = new SessionsResult(parseObject(response), task.getPrincipal(), task.getTimezone());
			pageToken = result.getNextPageToken();
			if (!events.addAll(result.getEvents())) {
				break;
			}
		} while (pageToken != null);
		return events;
	}

	private List<Event> createEventsFromActivities(GoogleFitTask task, OAuthCredentials credentials, Map<String, DataStream> streams) {
		List<Event> events = Lists.newArrayList();
		DataStream stream = streams.get("derived:com.google.activity.segment:com.google.android.gms:merge_activity_segments");
		if (stream != null) {
			for (DataPoint point : getDataPoints(task, credentials, stream)) {
				Preconditions.checkState("com.google.activity.segment".equals(point.getDataType()));
				Event event = new Event();
				event.setValue(Event.TIMESTAMP, point.getBegin());
				if (point.isRange()) {
					event.addValue(Event.TIMESTAMP, point.getEnd());
					event.setValue(Event.DURATION, point.getDuration());
				}
				event.addValue(Event.TAG, ActivityTypes.forID(point.getValue(0).intValueExact()));
				event.setValue(Event.AUTHOR, task.getPrincipal());
				DataStream origin = streams.get(point.getOrigin());
				if (origin != null) {
					event.setValue(Event.SOURCE, origin.getSource());
				}
				events.add(event);
			}
		}
		return events;
	}

	private List<DataPoint> getDataPoints(GoogleFitTask task, OAuthCredentials credentials, DataStream stream) {
		DateTime begin = task.getFrom();
		DateTime end = DateTime.now().minusHours(1);
		OAuthRequest request = new OAuthRequest(Verb.GET, String.format("https://www.googleapis.com/fitness/v1/users/me/dataSources/%s/datasets/%d-%d",
			UrlEscapers.urlPathSegmentEscaper().escape(stream.getId()), begin.getMillis() * 1000000, end.getMillis() * 1000000));
		Response response = send(request, credentials);
		return new DatasetResult(parseObject(response), task.getTimezone(), begin).getDataPoints();
	}

	private void addLocation(GoogleFitTask task, OAuthCredentials credentials, DataStream stream, List<Event> events) {
		if (stream != null) {
			RangeMap<DateTime, Location> locations = TreeRangeMap.create();
			Location beginLocation = null;
			DateTime begin = null;
			for (DataPoint point : getDataPoints(task, credentials, stream)) {
				Preconditions.checkState("com.google.location.sample".equals(point.getDataType()));
				Preconditions.checkState(!point.isRange());
				Location location = new Location(point.getValue(0), point.getValue(1));
				if (begin != null) {
					locations.put(Range.openClosed(begin, point.getEnd()), beginLocation);
				}
				begin = point.getEnd();
				beginLocation = location;
			}
			for (Event event : events) {
				Range<DateTime> range = getRange(event);
				Collection<Location> matches = locations.subRangeMap(range).asMapOfRanges().values();
				if (!matches.isEmpty()) {
					event.setValue(Event.LOCATION, matches.iterator().next());
				}
			}
		}
	}

	private static Range<DateTime> getRange(Event event) {
		ImmutableList<DateTime> values = event.getValues(Event.TIMESTAMP);
		return Range.closed(Ordering.natural().min(values), Ordering.natural().max(values));
	}

	private void addDistance(GoogleFitTask task, OAuthCredentials credentials, DataStream stream, List<Event> events) {
		if (stream != null) {
			RangeMap<DateTime, BigDecimal> values = TreeRangeMap.create();
			for (DataPoint point : getDataPoints(task, credentials, stream)) {
				Preconditions.checkState("com.google.distance.delta".equals(point.getDataType()));
				BigDecimal value = point.getValue(0);
				if (value.compareTo(BigDecimal.ZERO) > 0) {
					values.put(Range.closed(point.getBegin(), point.getEnd()), value);
				}
			}
			for (Event event : events) {
				Range<DateTime> range = getRange(event);
				double sum = sum(values.subRangeMap(range).asMapOfRanges().values());
				if (sum > 0.0) {
					Unit<Length> unit = task.isMetric() ? Units.KM : Units.MI;
					event.setValue(Event.DISTANCE, Measures.valueOf(Measures.convert(sum, unit), unit));
				}
			}
		}
	}

	private static double sum(Iterable<BigDecimal> values) {
		double sum = 0.0;
		for (BigDecimal value : values) {
			sum += value.doubleValue();
		}
		return sum;
	}

	private void addCount(GoogleFitTask task, OAuthCredentials credentials, DataStream stream, List<Event> events) {
		if (stream != null) {
			RangeMap<DateTime, BigDecimal> values = TreeRangeMap.create();
			for (DataPoint point : getDataPoints(task, credentials, stream)) {
				Preconditions.checkState("com.google.step_count.delta".equals(point.getDataType()));
				BigDecimal value = point.getValue(0);
				if (value.compareTo(BigDecimal.ZERO) > 0) {
					values.put(Range.closed(point.getBegin(), point.getEnd()), value);
				}
			}
			if (!values.asMapOfRanges().isEmpty()) {
				for (Event event : events) {
					Range<DateTime> range = getRange(event);
					int value = sumInts(values.subRangeMap(range).asMapOfRanges().values());
					if (value > 0) {
						event.setValue(Event.COUNT, value);
					}
				}
			}
		}
	}

	private static int sumInts(Iterable<BigDecimal> values) {
		int sum = 0;
		for (BigDecimal value : values) {
			sum += value.intValueExact();
		}
		return sum;
	}

	private void addVelocity(GoogleFitTask task, OAuthCredentials credentials, Iterable<DataStream> streams, List<Event> events) {
		RangeMap<DateTime, BigDecimal> values = TreeRangeMap.create();
		for (DataStream stream : streams) {
			for (DataPoint point : getDataPoints(task, credentials, stream)) {
				BigDecimal value = point.getValue(0);
				if (value.compareTo(BigDecimal.ZERO) > 0) {
					values.put(Range.closed(point.getBegin(), point.getEnd()), value);
				}
			}
		}
		if (!values.asMapOfRanges().isEmpty()) {
			for (Event event : events) {
				Range<DateTime> range = getRange(event);
				BigDecimal value = values.asMapOfRanges().get(range);
				if (value != null) {
					Unit<Velocity> unit = task.isMetric() ? Units.KMH : Units.MPH;
					event.setValue(Event.VELOCITY, Measures.valueOf(Measures.convert(value.doubleValue(), unit), unit));
				}
			}
		}
	}

	private void addEnergy(GoogleFitTask task, OAuthCredentials credentials, Iterable<DataStream> streams, List<Event> events) {
		RangeMap<DateTime, BigDecimal> values = TreeRangeMap.create();
		for (DataStream stream : streams) {
			for (DataPoint point : getDataPoints(task, credentials, stream)) {
				BigDecimal value = point.getValue(0);
				if (value.compareTo(BigDecimal.ZERO) > 0) {
					values.put(Range.closed(point.getBegin(), point.getEnd()), value);
				}
			}
		}
		if (!values.asMapOfRanges().isEmpty()) {
			for (Event event : events) {
				Range<DateTime> range = getRange(event);
				BigDecimal value = values.asMapOfRanges().get(range);
				if (value != null) {
					event.setValue(Event.ENERGY, Measures.valueOf(value, Units.KCAL));
				}
			}
		}
	}

	private void addFrequency(GoogleFitTask task, OAuthCredentials credentials, Iterable<DataStream> streams, List<Event> events) {
		RangeMap<DateTime, BigDecimal> values = TreeRangeMap.create();
		for (DataStream stream : streams) {
			for (DataPoint point : getDataPoints(task, credentials, stream)) {
				BigDecimal value = point.getValue(0);
				if (value.compareTo(BigDecimal.ZERO) > 0) {
					values.put(Range.closed(point.getBegin(), point.getEnd()), value);
				}
			}
		}
		if (!values.asMapOfRanges().isEmpty()) {
			for (Event event : events) {
				Range<DateTime> range = getRange(event);
				BigDecimal value = values.asMapOfRanges().get(range);
				if (value != null) {
					event.setValue(Event.FREQUENCY, Measures.valueOf(value, Units.BPM));
				}
			}
		}
	}

	private Iterable<DataStream> filter(Iterable<DataStream> streams, final String dataType) {
		return Iterables.filter(streams, new Predicate<DataStream>() {
			@Override
			public boolean apply(DataStream stream) {
				return dataType.equals(stream.getDataType());
			}
		});
	}

	private void setDefaultSource(List<Event> events) {
		for (Event event : events) {
			if (!event.contains(Event.SOURCE)) {
				event.setValue(Event.SOURCE, DEFAULT_SOURCE);
			}
		}
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
				latest = end;
			}
		}
		return latest != null ? latest.toString() : null;
	}
}
