package com.zenobase.tasks.jawbone;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class StepsResult extends JawboneResult {

	private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormat.forPattern("yyyyMMddHH");

	private final boolean hourly;
	private final boolean metric;

	public StepsResult(JsonNode node, Identity author, String tag, boolean hourly, boolean metric) {
		super(node, author, tag);
		this.hourly = hourly;
		this.metric = metric;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		JsonNode itemsNode = node.path("items");
		boolean more = next() != null;
		for (int i = 0; i < itemsNode.size(); ++i) {
			if (more || i < itemsNode.size() - 1) {
				if (hourly) {
					addHourEvents(itemsNode.get(i), events);
				} else {
					events.add(newDayEvent(itemsNode.get(i)));
				}
			}
		}
		return events;
	}

	private Event newDayEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		DateTimeZone zone = dateTimeZoneValue(node.path("details").path("tz"));
		DateTime begin = dateTimeValue(node.path("time_created"), zone);
		DateTime end = dateTimeValue(node.path("time_completed"), zone);
		event.setValue(Event.TIMESTAMP, begin);
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.setValue(Event.COUNT, node.path("details").path("steps").intValue());
		event.setValue(Event.DISTANCE, distanceValue(node.path("details").path("distance"), metric ? Units.KM : Units.MI));
		event.setValue(Event.ENERGY, energyValue(node.path("details").path("calories")));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private void addHourEvents(JsonNode node, List<Event> events) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		DateTimeZone zone = dateTimeZoneValue(node.path("details").path("tz"));
		for (Iterator<Map.Entry<String, JsonNode>> i = node.path("details").path("hourly_totals").fields(); i.hasNext();) {
			Map.Entry<String, JsonNode> field = i.next();
			events.add(newHourEvent(field.getKey(), field.getValue(), zone));
		}
	}

	private Event newHourEvent(String key, JsonNode node, DateTimeZone zone) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, DateTime.parse(key, HOUR_FORMAT.withZone(zone)));
		event.setValue(Event.DURATION, Duration.standardHours(1));
		event.setValue(Event.COUNT, node.path("steps").intValue());
		event.setValue(Event.DISTANCE, Measures.round(distanceValue(node.path("distance"), metric ? Units.M : Units.FT), 0));
		event.setValue(Event.ENERGY, energyValue(node.path("calories")));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}
}
