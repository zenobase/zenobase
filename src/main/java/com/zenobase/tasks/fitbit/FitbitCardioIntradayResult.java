package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.measure.DecimalMeasure;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.jspecify.annotations.Nullable;

class FitbitCardioIntradayResult extends FitbitResultSupport {

	private final LocalDate date;

	public FitbitCardioIntradayResult(
		JsonNode node,
		@Nullable String tag,
		Identity author,
		LocalDate date,
		DateTimeZone timezone
	) {
		super(node, tag, author, timezone);
		this.date = date;
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (Map.Entry<DateTime, Collection<Integer>> entry : valuesByHour().asMap().entrySet()) {
			events.add(toEvent(entry.getKey(), Objects.requireNonNull(mean(entry.getValue()))));
		}
		return events;
	}

	private static @Nullable BigDecimal mean(Collection<Integer> values) {
		int count = 0;
		int sum = 0;
		for (Integer value : values) {
			++count;
			sum += value;
		}
		return count > 0 ? BigDecimal.valueOf(sum / count) : null;
	}

	private ListMultimap<DateTime, Integer> valuesByHour() {
		LinkedListMultimap<DateTime, Integer> values = LinkedListMultimap.create();
		for (JsonNode recordNode : node.path("activities-heart-intraday").path("dataset")) {
			DateTime hour = toDateTimeFullHour(LocalTime.parse(recordNode.path("time").textValue()));
			if (hour != null) {
				values.put(hour, recordNode.path("value").intValue());
			}
		}
		return values;
	}

	private @Nullable DateTime toDateTimeFullHour(LocalTime local) {
		return toDateTimeFullHour(
			date.toLocalDateTime(local).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)
		);
	}

	private @Nullable DateTime toDateTimeFullHour(LocalDateTime local) {
		DateTimeZone tz = Objects.requireNonNull(timezone);
		return !tz.isLocalDateTimeGap(local) ? local.toDateTime(tz) : null;
	}

	private Event toEvent(DateTime timestamp, BigDecimal value) {
		var event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, timestamp);
		event.setValue(Event.DURATION, Duration.standardHours(1));
		event.setValue(Event.FREQUENCY, DecimalMeasure.valueOf(value, Units.BPM));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}
}
