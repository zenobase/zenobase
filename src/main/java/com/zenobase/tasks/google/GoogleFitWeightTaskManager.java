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
import java.util.Objects;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class GoogleFitWeightTaskManager extends GoogleFitTaskManagerSupport<GoogleFitWeightTask> {

	@Inject
	public GoogleFitWeightTaskManager(GoogleCredentialsManager credentialsManager) {
		super(GoogleFitWeightTask.TYPE, credentialsManager, GoogleFitWeightTask.class);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		DateTime begin = DateTime.parse(settings.path("marker").textValue()).withZoneRetainFields(zone);
		boolean metric = settings.path("metric").booleanValue();
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "Weight");
		return new GoogleFitWeightTask(bucketId, principal, zone, metric, tag, begin.toString());
	}

	@Override
	protected List<Event> createEvents(
		GoogleFitWeightTask task,
		OAuthCredentials credentials,
		Map<String, DataStream> streams
	) {
		List<Event> events = new ArrayList<>();
		DataStream stream = streams.get("derived:com.google.weight:com.google.android.gms:merge_weight");
		if (stream != null) {
			getDataPoints(task, credentials, stream, point -> {
				Event event = new Event();
				event.addValue(Event.TAG, task.getTag());
				if (point.isRange()) {
					event.addValue(Event.TIMESTAMP, point.getBegin());
					event.addValue(Event.TIMESTAMP, point.getEnd());
				} else {
					event.setValue(Event.TIMESTAMP, point.getBegin());
				}
				Unit<Mass> unit = task.isMetric() ? Units.KG : Units.LB;
				BigDecimal value = Objects.requireNonNull(
					Measures.convert(Objects.requireNonNull(point.getValue(0, BigDecimal.class)).doubleValue(), unit)
				);
				event.setValue(Event.WEIGHT, Measures.valueOf(value, unit));
				event.setValue(Event.AUTHOR, task.getPrincipal());
				DataStream origin = streams.get(point.getOrigin());
				if (origin != null) {
					event.setValue(Event.SOURCE, origin.source());
				}
				events.add(event);
			});
		}
		return events;
	}
}
