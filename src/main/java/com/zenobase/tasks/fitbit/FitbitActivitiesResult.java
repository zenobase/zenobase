package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import java.util.ArrayList;
import java.util.List;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;

class FitbitActivitiesResult extends FitbitResultSupport {

	private final boolean autodetected;
	private final Unit<Length> distanceUnit;

	public FitbitActivitiesResult(JsonNode node, Identity author, boolean autodetected, Unit<Length> distanceUnit) {
		super(node, null, author, null);
		this.autodetected = autodetected;
		this.distanceUnit = distanceUnit;
	}

	public @Nullable String next() {
		return Strings.emptyToNull(node.path("pagination").path("next").textValue());
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode item : node.path("activities")) {
			if (autodetected || !"auto_detected".equals(item.path("logType").textValue())) {
				DateTime time = DateTime.parse(item.path("startTime").textValue());
				Duration duration = durationValue(item.path("duration"));
				DecimalMeasure<Length> distance = lengthValue(item.path("distance"), distanceUnit);
				Event event = new Event();
				event.setValue(Event.TAG, item.path("activityName").textValue());
				event.setValue(Event.TIMESTAMP, time);
				event.setValue(Event.DURATION, duration);
				event.setValue(Event.COUNT, countValue(item.path("steps")));
				event.setValue(Event.DISTANCE, distance);
				event.setValue(
					Event.VELOCITY,
					velocityValue(item.path("speed"), Units.isMetric(distanceUnit) ? Units.KMH : Units.MPH)
				);
				event.setValue(
					Event.PACE,
					paceValue(item.path("pace"), Units.isMetric(distanceUnit) ? Units.S_PER_KM : Units.S_PER_MI)
				);
				event.setValue(Event.ENERGY, energyValue(item.path("calories"), Units.KCAL));
				event.setValue(Event.FREQUENCY, frequencyValue(item.path("averageHeartRate")));
				event.setValue(Event.AUTHOR, author);
				event.setValue(Event.SOURCE, SOURCE);
				events.add(event);
			}
		}
		return events;
	}
}
