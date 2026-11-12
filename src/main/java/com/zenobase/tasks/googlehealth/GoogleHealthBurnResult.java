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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;

/**
 * Parses responses from {@code totalCalories:dailyRollup} and {@code totalCalories:list} (hourly). The rollup shape
 * is {@code totalCaloriesRollupValues: [{ startTime, endTime, kilocalories }]}.
 */
class GoogleHealthBurnResult extends GoogleHealthResultSupport {

	GoogleHealthBurnResult(JsonNode node, @Nullable String tag, Identity author, DateTimeZone timezone) {
		super(node, tag, author, timezone);
	}

	List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode item : node.path("totalCaloriesRollupValues")) {
			BigDecimal kcal = item.path("kilocalories").decimalValue();
			if (kcal.signum() <= 0) {
				continue;
			}
			DateTime begin = dateTimeValue(item.path("startTime"), timezone);
			DateTime end = dateTimeValue(item.path("endTime"), timezone);
			Event event = new Event();
			if (tag != null) {
				event.setValue(Event.TAG, tag);
			}
			event.setValue(Event.TIMESTAMP, begin);
			if (!end.equals(begin)) {
				event.addValue(Event.TIMESTAMP, end);
				event.setValue(Event.DURATION, new Duration(begin, end));
			}
			event.setValue(Event.ENERGY, Measures.valueOf(kcal.setScale(0, RoundingMode.HALF_UP), Units.KCAL));
			event.setValue(Event.AUTHOR, author);
			setSources(event, item.path("origin"));
			events.add(event);
		}
		return events;
	}
}
