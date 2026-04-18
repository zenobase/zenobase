package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import java.util.ArrayList;
import java.util.List;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.jspecify.annotations.Nullable;

class FitbitWeightResult extends FitbitResultSupport {

	private final LocalDate date;
	private final Unit<Mass> weightUnit;

	public FitbitWeightResult(
		JsonNode node,
		@Nullable String tag,
		Identity author,
		LocalDate date,
		DateTimeZone timezone,
		Unit<Mass> weightUnit
	) {
		super(node, tag, author, timezone);
		this.date = date;
		this.weightUnit = weightUnit;
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		DecimalMeasure<Mass> weight = weightValue(node.path("body").path("weight"), weightUnit);
		if (weight != null) {
			DateTime begin = date.toDateTimeAtStartOfDay(timezone);
			Event event = new Event();
			event.setValue(Event.TAG, tag);
			event.setValue(Event.TIMESTAMP, begin);
			event.setValue(Event.WEIGHT, weight);
			event.setValue(Event.PERCENTAGE, percentageValue(node.path("body").path("fat")));
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
			events.add(event);
		}
		return events;
	}
}
