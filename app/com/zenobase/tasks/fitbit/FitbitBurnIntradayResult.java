package com.zenobase.tasks.fitbit;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.measure.DecimalMeasure;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class FitbitBurnIntradayResult extends FitbitResultSupport {

	private final LocalDate date;

	public FitbitBurnIntradayResult(JsonNode node, String tag, Identity author, LocalDate date, DateTimeZone timezone) {
		super(node, tag, author, timezone);
		this.date = date;
	}

	@Override
	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (Map.Entry<DateTime, Collection<BigDecimal>> entry : valuesByHour().asMap().entrySet()) {
			events.add(toEvent(entry.getKey(), sum(entry.getValue())));
		}
		return events;
	}

	private static BigDecimal sum(Iterable<BigDecimal> values) {
		BigDecimal sum = BigDecimal.ZERO;
		for (BigDecimal value : values) {
			sum = sum.add(value);
		}
		return sum;
	}

	private Multimap<DateTime, BigDecimal> valuesByHour() {
		Multimap<DateTime, BigDecimal> values = LinkedListMultimap.create();
		for (JsonNode recordNode : node.path("activities-calories-intraday").path("dataset")) {
			DateTime hour = toDateTimeFullHour(LocalTime.parse(recordNode.path("time").textValue()));
			if (hour != null) {
				values.put(hour, recordNode.path("value").decimalValue());
			}
		}
		return values;
	}

	private DateTime toDateTimeFullHour(LocalTime local) {
		return toDateTimeFullHour(date.toLocalDateTime(local).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0));
	}

	private DateTime toDateTimeFullHour(LocalDateTime local) {
		return !timezone.isLocalDateTimeGap(local) ? local.toDateTime(timezone) : null;
	}

	private Event toEvent(DateTime timestamp, BigDecimal value) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, timestamp);
		event.setValue(Event.DURATION, Duration.standardHours(1));
		event.setValue(Event.ENERGY, DecimalMeasure.valueOf(Measures.round(value, 0), Units.KCAL));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
