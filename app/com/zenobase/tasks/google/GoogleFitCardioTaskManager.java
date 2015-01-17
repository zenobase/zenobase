package com.zenobase.tasks.google;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class GoogleFitCardioTaskManager extends GoogleFitTaskManagerSupport<GoogleFitCardioTask> {

	@Inject
	public GoogleFitCardioTaskManager(GoogleCredentialsManager credentialsManager) {
		super(GoogleFitCardioTask.TYPE, credentialsManager, GoogleFitCardioTask.class);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		DateTime begin = DateTime.parse(settings.path("marker").textValue()).withZoneRetainFields(zone);
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "Cardio");
		return new GoogleFitCardioTask(bucketId, principal, zone, tag, begin.toString());
	}

	@Override
	protected List<Event> createEvents(GoogleFitCardioTask task, OAuthCredentials credentials, Map<String, DataStream> streams) {
		List<Event> events = Lists.newArrayList();
		for (DataStream stream : filter(streams.values(), "com.google.heart_rate.bpm", "com.google.heart_rate.summary")) {
			if (!stream.getId().contains("derived")) {
				for (DataPoint point : getDataPoints(task, credentials, stream)) {
					BigDecimal value = point.getValue(0);
					if (BigDecimal.ZERO.compareTo(value) < 0) {
						Event event = new Event();
						event.addValue(Event.TAG, task.getTag());
						event.setValue(Event.TIMESTAMP, point.getBegin());
						if (point.isRange()) {
							event.addValue(Event.TIMESTAMP, point.getEnd());
							event.setValue(Event.DURATION, point.getDuration());
						}
						event.setValue(Event.FREQUENCY, Measures.valueOf(Measures.round(value, 0), Units.BPM));
						event.setValue(Event.AUTHOR, task.getPrincipal());
						event.setValue(Event.SOURCE, stream.getSource());
						events.add(event);
					}
				}
			}
		}
		return events;
	}
}
