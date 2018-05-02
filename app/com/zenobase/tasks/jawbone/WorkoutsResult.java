package com.zenobase.tasks.jawbone;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import play.Logger;

import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class WorkoutsResult extends JawboneResult {

	private static Map<Integer, String> TYPES = ImmutableMap.<Integer, String>builder()
		.put(1, "walk")
		.put(2, "run")
		.put(3, "lift weights")
		.put(4, "cross train")
		.put(5, "nike training")
		.put(6, "yoga")
		.put(7, "pilates")
		.put(8, "body weight exercise")
		.put(9, "crossfit")
		.put(10, "p90x")
		.put(11, "zumba")
		.put(12, "trx")
		.put(13, "swim")
		.put(14, "bike")
		.put(15, "elliptical")
		.put(16, "bar method")
		.put(17, "kinect exercises")
		.put(18, "tennis")
		.put(19, "basketball")
		.put(20, "golf")
		.put(21, "soccer")
		.put(22, "ski snowboard")
		.put(23, "dance")
		.put(24, "hike")
		.put(25, "cross country skiing")
		.put(26, "stationary bike")
		.put(27, "cardio")
		.put(28, "game")
		.put(29, "other")
		.build();

	private final boolean metric;

	public WorkoutsResult(JsonNode node, Identity author, boolean metric) {
		super(node, author, null);
		this.metric = metric;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode itemNode : node.path("items")) {
			events.add(newEvent(itemNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, getType(node.path("sub_type")));
		DateTimeZone zone = dateTimeZoneValue(node.path("details").path("tz"));
		DateTime begin = dateTimeValue(node.path("time_created"), zone);
		DateTime end = dateTimeValue(node.path("time_completed"), zone);
		event.setValue(Event.TIMESTAMP, begin);
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.setValue(Event.LOCATION, locationValue(node));
		event.setValue(Event.COUNT, node.path("details").path("steps").intValue());
		event.setValue(Event.DISTANCE, distanceValue(node.path("details").path("meters"), metric ? Units.KM : Units.MI));
		event.setValue(Event.ENERGY, energyValue(node.path("details").path("calories")));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private static String getType(JsonNode typeNode) {
		String type = TYPES.get(typeNode.intValue());
		if (type == null && !typeNode.isNull()) {
			Logger.warn("Unknown workout type: {}", typeNode);
		}
		return Objects.firstNonNull(type, "workout");
	}
}
