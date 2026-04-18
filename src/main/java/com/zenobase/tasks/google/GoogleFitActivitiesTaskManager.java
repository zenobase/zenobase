package com.zenobase.tasks.google;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.common.math.Stats;
import com.zenobase.common.Measures;
import com.zenobase.common.Pace;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleFitActivitiesTaskManager extends GoogleFitTaskManagerSupport<GoogleFitActivitiesTask> {

	private static final Logger logger = LoggerFactory.getLogger(GoogleFitActivitiesTaskManager.class);

	@Inject
	public GoogleFitActivitiesTaskManager(GoogleCredentialsManager credentialsManager) {
		super(GoogleFitActivitiesTask.TYPE, credentialsManager, GoogleFitActivitiesTask.class);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		DateTime begin = DateTime.parse(settings.path("marker").textValue()).withZoneRetainFields(zone);
		boolean metric = settings.path("metric").booleanValue();
		boolean derived = settings.path("derived").booleanValue();
		return new GoogleFitActivitiesTask(bucketId, principal, zone, metric, derived, begin.toString());
	}

	@Override
	protected List<Event> createEvents(
		GoogleFitActivitiesTask task,
		OAuthCredentials credentials,
		Map<String, DataStream> streams
	) {
		List<Event> events = task.useDerived()
			? createEventsFromActivities(task, credentials, streams)
			: createEventsFromSessions(task, credentials);

		if (!events.isEmpty()) {
			processDistanceCumulative(
				task,
				credentials,
				filter(streams.values(), "com.google.distance.cumulative"),
				events
			);
			processDistanceDelta(
				task,
				credentials,
				Objects.requireNonNull(
					streams.get("derived:com.google.distance.delta:com.google.android.gms:pruned_distance")
				),
				events
			);
			processStepCountDelta(
				task,
				credentials,
				Objects.requireNonNull(
					streams.get("derived:com.google.step_count.delta:com.google.android.gms:merge_step_deltas")
				),
				events
			);
			processSpeedSummary(task, credentials, filter(streams.values(), "com.google.speed.summary"), events);
			processCaloriesExpended(
				task,
				credentials,
				filter(streams.values(), "com.google.calories.expended"),
				events
			);
			processHeartRateSummary(
				task,
				credentials,
				filter(streams.values(), "com.google.heart_rate.summary"),
				events
			);
			processHeartRate(task, credentials, filter(streams.values(), "com.google.heart_rate.bpm"), events);
		}

		return events;
	}

	private List<Event> createEventsFromSessions(GoogleFitActivitiesTask task, OAuthCredentials credentials) {
		List<Event> events = new ArrayList<>();
		String pageToken = null;
		do {
			OAuthRequest request = new OAuthRequest(
				Verb.GET,
				"https://www.googleapis.com/fitness/v1/users/me/sessions"
			);
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
		} while (!pageToken.isEmpty());
		return events;
	}

	private List<Event> createEventsFromActivities(
		GoogleFitActivitiesTask task,
		OAuthCredentials credentials,
		Map<String, DataStream> streams
	) {
		List<Event> events = new ArrayList<>();
		DataStream stream = streams.get(
			"derived:com.google.activity.segment:com.google.android.gms:merge_activity_segments"
		);
		if (stream != null) {
			getDataPoints(task, credentials, stream, point -> {
				Preconditions.checkState("com.google.activity.segment".equals(point.getDataType()));
				Event event = new Event();
				event.setValue(Event.TIMESTAMP, point.getBegin());
				if (point.isRange()) {
					event.addValue(Event.TIMESTAMP, point.getEnd());
					event.setValue(Event.DURATION, point.getDuration());
				}
				String tag = ActivityTypes.forID(
					Objects.requireNonNull(point.getValue(0, BigDecimal.class)).intValueExact()
				);
				if (tag != null) {
					event.addValue(Event.TAG, tag);
				} else {
					logger.warn("Unknown activity type: {}", point.getValue(0));
				}
				event.setValue(Event.AUTHOR, task.getPrincipal());
				DataStream origin = streams.get(point.getOrigin());
				if (origin != null) {
					event.setValue(Event.SOURCE, origin.source());
				}
				events.add(event);
			});
		}
		if (!events.isEmpty()) {
			events.removeLast(); // could still be in progress
		}
		return events;
	}

	private void processDistanceCumulative(
		GoogleFitActivitiesTask task,
		OAuthCredentials credentials,
		Iterable<DataStream> streams,
		List<Event> events
	) {
		RangeMap<DateTime, BigDecimal> values = TreeRangeMap.create();
		for (DataStream stream : streams) {
			getDataPoints(task, credentials, stream, point -> {
				Preconditions.checkState("com.google.distance.cumulative".equals(point.getDataType()));
				BigDecimal value = Objects.requireNonNull(point.getValue(0, BigDecimal.class));
				if (value.compareTo(BigDecimal.ZERO) > 0) {
					values.put(Range.closed(point.getBegin(), point.getEnd()), value);
				}
			});
		}
		if (!values.asMapOfRanges().isEmpty()) {
			for (Event event : events) {
				Range<DateTime> range = getRange(event);
				BigDecimal value = values.asMapOfRanges().get(range);
				if (value != null) {
					Unit<Length> unit = task.isMetric() ? Units.KM : Units.MI;
					event.setValue(
						Event.DISTANCE,
						Measures.valueOf(Objects.requireNonNull(Measures.convert(value.doubleValue(), unit)), unit)
					);
				}
			}
		}
	}

	private void processDistanceDelta(
		GoogleFitActivitiesTask task,
		OAuthCredentials credentials,
		DataStream stream,
		List<Event> events
	) {
		if (stream != null) {
			RangeMap<DateTime, BigDecimal> values = TreeRangeMap.create();
			getDataPoints(task, credentials, stream, point -> {
				Preconditions.checkState("com.google.distance.delta".equals(point.getDataType()));
				BigDecimal value = Objects.requireNonNull(point.getValue(0, BigDecimal.class));
				if (value.compareTo(BigDecimal.ZERO) > 0) {
					values.put(Range.closed(point.getBegin(), point.getEnd()), value);
				}
			});
			for (Event event : events) {
				if (!event.contains(Event.DISTANCE)) {
					Range<DateTime> range = getRange(event);
					double sum = sum(values.subRangeMap(range).asMapOfRanges().values());
					if (sum > 0.0) {
						Unit<Length> unit = task.isMetric() ? Units.KM : Units.MI;
						event.setValue(
							Event.DISTANCE,
							Measures.valueOf(Objects.requireNonNull(Measures.convert(sum, unit)), unit)
						);
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

	private void processStepCountDelta(
		GoogleFitActivitiesTask task,
		OAuthCredentials credentials,
		DataStream stream,
		List<Event> events
	) {
		if (stream != null) {
			RangeMap<DateTime, BigDecimal> values = TreeRangeMap.create();
			getDataPoints(task, credentials, stream, point -> {
				Preconditions.checkState("com.google.step_count.delta".equals(point.getDataType()));
				BigDecimal value = Objects.requireNonNull(point.getValue(0, BigDecimal.class));
				if (value.compareTo(BigDecimal.ZERO) > 0) {
					values.put(Range.closed(point.getBegin(), point.getEnd()), value);
				}
			});
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

	private void processSpeedSummary(
		GoogleFitActivitiesTask task,
		OAuthCredentials credentials,
		Iterable<DataStream> streams,
		List<Event> events
	) {
		RangeMap<DateTime, BigDecimal> values = TreeRangeMap.create();
		for (DataStream stream : streams) {
			getDataPoints(task, credentials, stream, point -> {
				Preconditions.checkState("com.google.speed.summary".equals(point.getDataType()));
				BigDecimal value = Objects.requireNonNull(point.getValue(0, BigDecimal.class));
				if (value.compareTo(BigDecimal.ZERO) > 0) {
					values.put(Range.closed(point.getBegin(), point.getEnd()), value);
				}
			});
		}
		if (!values.asMapOfRanges().isEmpty()) {
			for (Event event : events) {
				Range<DateTime> range = getRange(event);
				BigDecimal value = values.asMapOfRanges().get(range);
				if (value != null) {
					Unit<Velocity> unit = task.isMetric() ? Units.KMH : Units.MPH;
					event.setValue(
						Event.VELOCITY,
						Measures.valueOf(
							Objects.requireNonNull(Measures.round(Measures.convert(value.doubleValue(), unit), 1)),
							unit
						)
					);
					if (value.doubleValue() > 0.0) {
						Unit<Pace> paceUnit = task.isMetric() ? Units.S_PER_KM : Units.S_PER_MI;
						event.setValue(
							Event.PACE,
							Measures.valueOf(
								Objects.requireNonNull(
									Measures.round(Measures.convert(Math.pow(value.doubleValue(), -1), paceUnit), 0)
								),
								paceUnit
							)
						);
					}
				}
			}
		}
	}

	private void processCaloriesExpended(
		GoogleFitActivitiesTask task,
		OAuthCredentials credentials,
		Iterable<DataStream> streams,
		List<Event> events
	) {
		RangeMap<DateTime, BigDecimal> values = TreeRangeMap.create();
		for (DataStream stream : streams) {
			getDataPoints(task, credentials, stream, point -> {
				Preconditions.checkState("com.google.calories.expended".equals(point.getDataType()));
				BigDecimal value = Objects.requireNonNull(point.getValue(0, BigDecimal.class));
				if (value.compareTo(BigDecimal.ZERO) > 0) {
					values.put(Range.closed(point.getBegin(), point.getEnd()), value);
				}
			});
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

	private void processHeartRateSummary(
		GoogleFitActivitiesTask task,
		OAuthCredentials credentials,
		Iterable<DataStream> streams,
		List<Event> events
	) {
		RangeMap<DateTime, BigDecimal> values = TreeRangeMap.create();
		for (DataStream stream : streams) {
			getDataPoints(task, credentials, stream, point -> {
				Preconditions.checkState("com.google.heart_rate.summary".equals(point.getDataType()));
				BigDecimal value = Objects.requireNonNull(point.getValue(0, BigDecimal.class));
				if (value.compareTo(BigDecimal.ZERO) > 0) {
					values.put(Range.closed(point.getBegin(), point.getEnd()), value);
				}
			});
		}
		if (!values.asMapOfRanges().isEmpty()) {
			for (Event event : events) {
				Range<DateTime> range = getRange(event);
				BigDecimal value = values.asMapOfRanges().get(range);
				if (value != null) {
					event.setValue(
						Event.FREQUENCY,
						Measures.valueOf(Objects.requireNonNull(Measures.round(value, 0)), Units.BPM)
					);
				}
			}
		}
	}

	private void processHeartRate(
		GoogleFitActivitiesTask task,
		OAuthCredentials credentials,
		Iterable<DataStream> streams,
		List<Event> events
	) {
		for (DataStream stream : streams) {
			RangeMap<DateTime, Integer> values = TreeRangeMap.create();
			getDataPoints(task, credentials, stream, point -> {
				Preconditions.checkState("com.google.heart_rate.bpm".equals(point.getDataType()));
				int value = Objects.requireNonNull(point.getValue(0, BigDecimal.class)).intValue();
				if (value > 0) {
					values.put(Range.closed(point.getBegin(), point.getEnd()), value);
				}
			});
			for (Event event : events) {
				if (!event.contains(Event.FREQUENCY)) {
					Range<DateTime> range = getRange(event);
					BigDecimal value = mean(values.subRangeMap(range).asMapOfRanges().values());
					if (value != null) {
						event.setValue(
							Event.FREQUENCY,
							Measures.valueOf(Objects.requireNonNull(Measures.round(value, 0)), Units.BPM)
						);
					}
				}
			}
		}
	}

	private static @Nullable BigDecimal mean(Iterable<? extends Number> values) {
		return !Iterables.isEmpty(values) ? new BigDecimal(Stats.meanOf(values)) : null;
	}
}
