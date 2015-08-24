package com.zenobase.tasks.google;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class GoogleFitFoodTaskManager extends GoogleFitTaskManagerSupport<GoogleFitFoodTask> {

	@Inject
	public GoogleFitFoodTaskManager(GoogleCredentialsManager credentialsManager) {
		super(GoogleFitFoodTask.TYPE, credentialsManager, GoogleFitFoodTask.class);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		DateTime begin = DateTime.parse(settings.path("marker").textValue()).withZoneRetainFields(zone);
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "Weight");
		return new GoogleFitFoodTask(bucketId, principal, zone, tag, begin.toString());
	}

	@Override
	protected List<Event> createEvents(GoogleFitFoodTask task, OAuthCredentials credentials, Map<String, DataStream> streams) {
		List<Event> events = Lists.newArrayList();
		DataStream stream = streams.get("derived:com.google.calories.consumed:com.google.android.gms:merge_calories_consumed");
		if (stream != null) {
			for (DataPoint point : getDataPoints(task, credentials, stream)) {
				Event event = new Event();
				event.addValue(Event.TAG, task.getTag());
				if (point.isRange()) {
					event.addValue(Event.TIMESTAMP, point.getBegin());
					event.addValue(Event.TIMESTAMP, point.getEnd());
				} else {
					event.setValue(Event.TIMESTAMP, point.getBegin());
				}
				event.setValue(Event.ENERGY, Measures.valueOf(point.getValue(0), Units.KCAL));
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
}
