package com.zenobase.tasks.google;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class GoogleFitFoodTaskManager extends GoogleFitTaskManagerSupport<GoogleFitFoodTask> {

	@Inject
	public GoogleFitFoodTaskManager(GoogleCredentialsManager credentialsManager) {
		super(GoogleFitFoodTask.TYPE, credentialsManager, GoogleFitFoodTask.class);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		DateTime begin = DateTime.parse(settings.path("marker").textValue()).withZoneRetainFields(zone);
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "Food");
		return new GoogleFitFoodTask(bucketId, principal, zone, tag, begin.toString());
	}

	@Override
	protected List<Event> createEvents(
		GoogleFitFoodTask task,
		OAuthCredentials credentials,
		Map<String, DataStream> streams
	) {
		List<Event> events = createEventsFromNutritionStreams(task, credentials, streams);
		if (events.isEmpty()) {
			events = createEventsFromLegacyStream(task, credentials, streams);
		}
		return events;
	}

	private List<Event> createEventsFromNutritionStreams(
		GoogleFitFoodTask task,
		OAuthCredentials credentials,
		Map<String, DataStream> streams
	) {
		List<Event> events = new ArrayList<>();
		for (DataStream stream : filter(streams.values(), "com.google.nutrition")) {
			getDataPoints(task, credentials, stream, point -> {
				if (
					point.getValue(0) instanceof Map<?, ?> values && values.get("calories") instanceof BigDecimal value
				) {
					Event event = newEvent(point, task, streams);
					event.setValue(Event.ENERGY, Measures.valueOf(value, Units.KCAL));
					events.add(event);
				}
			});
		}
		return events;
	}

	private List<Event> createEventsFromLegacyStream(
		GoogleFitFoodTask task,
		OAuthCredentials credentials,
		Map<String, DataStream> streams
	) {
		List<Event> events = new ArrayList<>();
		DataStream stream = streams.get(
			"derived:com.google.calories.consumed:com.google.android.gms:merge_calories_consumed"
		);
		if (stream != null) {
			getDataPoints(task, credentials, stream, point -> {
				BigDecimal value = point.getValue(0, BigDecimal.class);
				if (value != null) {
					Event event = newEvent(point, task, streams);
					event.setValue(Event.ENERGY, Measures.valueOf(value, Units.KCAL));
					events.add(event);
				}
			});
		}
		return events;
	}

	private static Event newEvent(DataPoint point, GoogleFitFoodTask task, Map<String, DataStream> streams) {
		var event = new Event();
		event.addValue(Event.TAG, task.getTag());
		if (point.isRange()) {
			event.addValue(Event.TIMESTAMP, point.getBegin());
			event.addValue(Event.TIMESTAMP, point.getEnd());
		} else {
			event.setValue(Event.TIMESTAMP, point.getBegin());
		}
		event.setValue(Event.AUTHOR, task.getPrincipal());
		DataStream origin = streams.get(point.getOrigin());
		if (origin != null) {
			event.setValue(Event.SOURCE, origin.source());
		}
		return event;
	}
}
