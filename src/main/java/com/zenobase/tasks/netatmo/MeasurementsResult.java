package com.zenobase.tasks.netatmo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;

class MeasurementsResult {

	public static final Resource SOURCE = new Resource("Netatmo", "https://www.netatmo.com/");

	private final JsonNode node;
	private final Identity author;
	private final Device device;
	private final boolean hourly;

	public MeasurementsResult(JsonNode node, Identity author, Device device, boolean hourly) {
		this.node = Preconditions.checkNotNull(node);
		this.author = Preconditions.checkNotNull(author);
		this.device = Preconditions.checkNotNull(device);
		this.hourly = hourly;
	}

	public boolean isSuccess() {
		return "ok".equals(node.path("status").textValue());
	}

	public List<Event> getEvents() {
		Preconditions.checkState(isSuccess(), "Expected a successful response but got <%s>", node);
		List<Event> events = new ArrayList<>();
		for (Map.Entry<String, JsonNode> stringJsonNodeEntry : node.path("body").properties()) {
			events.add(getEvent(stringJsonNodeEntry));
		}
		return events;
	}

	public Event getEvent(Map.Entry<String, JsonNode> entry) {
		ArrayNode node = (ArrayNode) entry.getValue();
		var event = new Event();
		DateTime timestamp = new DateTime(
				Long.parseLong(entry.getKey()) * 1000, device.getUpdated().getZone());
		if (hourly) {
			event.setValue(Event.TIMESTAMP, timestamp.withMinuteOfHour(0));
			event.setValue(Event.DURATION, Duration.standardHours(1));
		} else {
			event.setValue(Event.TIMESTAMP, timestamp);
		}
		event.addValue(Event.TAG, device.getLabel());
		event.setValue(Event.LOCATION, device.getLocation());
		if (device.supports("Temperature")) {
			event.setValue(Event.TEMPERATURE, getMeasure(node.get(0), Units.C));
		}
		if (device.supports("Pressure")) {
			event.setValue(Event.PRESSURE, getMeasure(node.get(1), Units.HPA));
		}
		if (device.supports("Noise")) {
			event.setValue(Event.SOUND, getMeasure(node.get(2), Units.DB));
		}
		if (device.supports("Humidity")) {
			event.setValue(Event.HUMIDITY, getInteger(node.get(3)));
		}
		if (device.supports("CO2")) {
			event.setValue(Event.RATING, getRating(node.get(4)));
		}
		if (device.supports("Wind")) {
			event.setValue(Event.VELOCITY, getMeasure(node.get(5), Units.KMH));
		}
		if (device.supports("Rain")) {
			event.setValue(Event.HEIGHT, getMeasure(node.get(6), Units.MM));
		}
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private static <Q extends Quantity> @Nullable DecimalMeasure<Q> getMeasure(JsonNode node, Unit<Q> unit) {
		return node.isNumber() ? Measures.valueOf(node.decimalValue(), unit) : null;
	}

	private static @Nullable Integer getInteger(JsonNode node) {
		return node.isNumber() ? node.intValue() : null;
	}

	private static @Nullable Rating getRating(JsonNode node) {
		return node.isNumber() ? Rating.valueOf(getRating(node.intValue())) : null;
	}

	/**
	 * @see <a href="http://www.engineeringtoolbox.com/co2-comfort-level-d_1024.html">CO2 Comfort Levels</a>
	 */
	private static int getRating(int value) {
		if (value < 450) { // outdoor
			return 100;
		}
		if (value < 600) { // ok
			return 80;
		}
		if (value < 800) {
			return 60;
		}
		if (value < 1000) { // drowsy
			return 40;
		}
		if (value < 2500) { // hazard
			return 20;
		}
		return 0;
	}
}
