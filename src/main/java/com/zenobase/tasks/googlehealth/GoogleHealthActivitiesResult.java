package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

/**
 * Parses responses from {@code exercise:list}: each entry is an exercise session with activity type, start/end, and
 * aggregated distance/energy metrics.
 */
class GoogleHealthActivitiesResult extends GoogleHealthResultSupport {

	private final boolean metric;

	GoogleHealthActivitiesResult(JsonNode node, Identity author, DateTimeZone timezone, boolean metric) {
		super(node, null, author, timezone);
		this.metric = metric;
	}

	List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode session : node.path("exerciseSessions")) {
			DateTime begin = dateTimeValue(session.path("startTime"), timezone);
			DateTime end = dateTimeValue(session.path("endTime"), timezone);
			Event event = new Event();
			event.setValue(Event.TIMESTAMP, begin);
			if (!end.equals(begin)) {
				event.addValue(Event.TIMESTAMP, end);
				event.setValue(Event.DURATION, new Duration(begin, end));
			}
			String activity = session.path("activityType").asText();
			if (!activity.isEmpty()) {
				event.addValue(Event.TAG, activity.toLowerCase(Locale.ROOT));
			}
			BigDecimal meters = session.path("distance").path("meters").decimalValue();
			if (meters.signum() > 0) {
				Unit<Length> unit = metric ? Units.KM : Units.MI;
				event.setValue(
					Event.DISTANCE,
					Measures.valueOf(Objects.requireNonNull(Measures.convert(meters.doubleValue(), unit)), unit)
				);
			}
			JsonNode calories = session.path("totalCalories").path("kilocalories");
			if (calories.isNumber() && calories.doubleValue() > 0.0) {
				event.setValue(
					Event.ENERGY,
					Measures.valueOf(calories.decimalValue().setScale(0, RoundingMode.HALF_UP), Units.KCAL)
				);
			}
			event.setValue(Event.AUTHOR, author);
			setSources(event, session.path("origin"));
			events.add(event);
		}
		return events;
	}
}
