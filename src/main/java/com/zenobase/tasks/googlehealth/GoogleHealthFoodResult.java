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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

/**
 * Parses responses from {@code nutrition:list}. Each entry is a logged nutrition/meal with a timestamp, a kilocalorie
 * value under {@code totalCalories}, and an optional {@code name} / {@code mealType}.
 */
class GoogleHealthFoodResult extends GoogleHealthResultSupport {

	GoogleHealthFoodResult(JsonNode node, @Nullable String tag, Identity author, DateTimeZone timezone) {
		super(node, tag, author, timezone);
	}

	List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode item : node.path("nutritionLogValues")) {
			DateTime timestamp = dateTimeValue(item.path("startTime"), timezone);
			Event event = new Event();
			event.addValue(Event.TAG, tag != null ? tag : "food");
			String name = item.path("name").textValue();
			if (name != null && !name.isEmpty()) {
				event.addValue(Event.TAG, name.toLowerCase(Locale.ROOT));
			}
			String mealType = item.path("mealType").textValue();
			if (mealType != null && !mealType.isEmpty()) {
				event.addValue(Event.TAG, mealType.toLowerCase(Locale.ROOT));
			}
			event.setValue(Event.TIMESTAMP, timestamp);
			BigDecimal kcal = item.path("totalCalories").path("kilocalories").decimalValue();
			if (kcal.signum() > 0) {
				event.setValue(Event.ENERGY, Measures.valueOf(kcal.setScale(0, RoundingMode.HALF_UP), Units.KCAL));
			}
			event.setValue(Event.AUTHOR, author);
			setSources(event, item.path("origin"));
			events.add(event);
		}
		return events;
	}
}
