package com.zenobase.tasks.google;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import com.zenobase.common.Measures;
import com.zenobase.common.Pace;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class GoogleFitActivitiesTaskManager extends GoogleFitTaskManagerSupport<GoogleFitActivitiesTask> {

	@Inject
	public GoogleFitActivitiesTaskManager(GoogleCredentialsManager credentialsManager) {
		super(GoogleFitActivitiesTask.TYPE, credentialsManager, GoogleFitActivitiesTask.class);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		DateTime begin = DateTime.parse(settings.path("marker").textValue()).withZoneRetainFields(zone);
		boolean metric = settings.path("metric").booleanValue();
		boolean derived = settings.path("derived").booleanValue();
		return new GoogleFitActivitiesTask(bucketId, principal, zone, metric, derived, begin.toString());
	}

	@Override
	protected List<Event> createEvents(GoogleFitActivitiesTask task, OAuthCredentials credentials, Map<String, DataStream> streams) {

		List<Event> events = task.useDerived()
			? createEventsFromActivities(task, credentials, streams)
			: createEventsFromSessions(task, credentials);

		if (!events.isEmpty()) {
			addLocation(task, credentials, streams.get("derived:com.google.location.sample:com.google.android.gms:merge_location_samples"), events);
			addDistance(task, credentials, filter(streams.values(), "com.google.distance.cumulative"), events);
			addDistance(task, credentials, streams.get("derived:com.google.distance.delta:com.google.android.gms:pruned_distance"), events);
			addCount(task, credentials, streams.get("derived:com.google.step_count.delta:com.google.android.gms:merge_step_deltas"), events);
			addVelocityAndPace(task, credentials, filter(streams.values(), "com.google.speed.summary"), events);
			addEnergy(task, credentials, filter(streams.values(), "com.google.calories.expended"), events);
			addFrequency(task, credentials, filter(streams.values(), "com.google.heart_rate.summary"), events);
		}

		return events;
	}

	private List<Event> createEventsFromSessions(GoogleFitActivitiesTask task, OAuthCredentials credentials) {
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

	private List<Event> createEventsFromActivities(GoogleFitActivitiesTask task, OAuthCredentials credentials, Map<String, DataStream> streams) {
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
		if (!events.isEmpty()) {
			events.remove(events.size() - 1); // could still be in progress
		}
		return events;
	}

	private void addLocation(GoogleFitActivitiesTask task, OAuthCredentials credentials, DataStream stream, List<Event> events) {
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

	private void addDistance(GoogleFitActivitiesTask task, OAuthCredentials credentials, Iterable<DataStream> streams, List<Event> events) {
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
					Unit<Length> unit = task.isMetric() ? Units.KM : Units.MI;
					event.setValue(Event.DISTANCE, Measures.valueOf(Measures.convert(value.doubleValue(), unit), unit));
				}
			}
		}
	}

	private void addDistance(GoogleFitActivitiesTask task, OAuthCredentials credentials, DataStream stream, List<Event> events) {
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
				if (!events.contains(Event.DISTANCE)) {
					Range<DateTime> range = getRange(event);
					double sum = sum(values.subRangeMap(range).asMapOfRanges().values());
					if (sum > 0.0) {
						Unit<Length> unit = task.isMetric() ? Units.KM : Units.MI;
						event.setValue(Event.DISTANCE, Measures.valueOf(Measures.convert(sum, unit), unit));
					}
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

	private void addCount(GoogleFitActivitiesTask task, OAuthCredentials credentials, DataStream stream, List<Event> events) {
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

	private void addVelocityAndPace(GoogleFitActivitiesTask task, OAuthCredentials credentials, Iterable<DataStream> streams, List<Event> events) {
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
					if (value.doubleValue() > 0.0) {
						Unit<Pace> paceUnit = task.isMetric() ? Units.S_PER_KM : Units.S_PER_MI;
						event.setValue(Event.PACE, Measures.valueOf(Measures.round(Measures.convert(Math.pow(value.doubleValue(), -1), paceUnit), 0), paceUnit));
					}
				}
			}
		}
	}

	private void addEnergy(GoogleFitActivitiesTask task, OAuthCredentials credentials, Iterable<DataStream> streams, List<Event> events) {
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
					event.setValue(Event.ENERGY, Measures.valueOf(value.setScale(0, RoundingMode.HALF_UP), Units.KCAL));
				}
			}
		}
	}

	private void addFrequency(GoogleFitActivitiesTask task, OAuthCredentials credentials, Iterable<DataStream> streams, List<Event> events) {
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
					event.setValue(Event.FREQUENCY, Measures.valueOf(Measures.round(value, 0), Units.BPM));
				}
			}
		}
	}
}
