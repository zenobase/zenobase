package com.zenobase.tasks.dash;

import java.util.List;
import java.util.UUID;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.quantity.Volume;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import com.zenobase.common.LengthPerVolume;
import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;

class TripsResult {

	static final Resource SOURCE = new Resource("Dash", "https://dash.by/");

	private final JsonNode node;
	private final Identity author;
	private final UserSettings settings;
	private final String tag;
	private final DateTimeZone timezone;

	public TripsResult(JsonNode node, Identity author, UserSettings settings, String tag, DateTimeZone timezone) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.settings = settings;
		this.tag = tag;
		this.timezone = timezone;
	}

	public List<Event> getTrips() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode resultNode : node.path("result")) {
			events.add(newEvent(resultNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TIMESTAMP, dateTimeValue(node.path("dateStart"), timezone));
		event.addValue(Event.TIMESTAMP, dateTimeValue(node.path("dateEnd"), timezone));
		event.setValue(Event.DURATION, durationValue(node.path("stats").path("timeDriven")));
		event.addValue(Event.TAG, tag);
		UUID vehicleId = uuidValue(node.path("vehicleId"));
		if (vehicleId != null) {
			event.addValue(Event.TAG, settings.getVehicle(vehicleId));
		}
		addLocationValue(event, node.path("startLatitude"), node.path("startLongitude"));
		addLocationValue(event, node.path("endLatitude"), node.path("endLongitude"));
		event.setValue(Event.RATING, ratingValue(node.path("score")));
		event.setValue(Event.DISTANCE, Measures.round(distanceValue(node.path("stats").path("distanceDriven"))));
		event.setValue(Event.VOLUME, Measures.round(volumeValue(node.path("stats").path("fuelConsumed"))));
		event.setValue(Event.DISTANCE_PER_VOLUME, Measures.round(fuelEfficiencyValue(node.path("stats").path("averageFuelEfficiency"))));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private static DateTime dateTimeValue(JsonNode node, DateTimeZone zone) {
		Preconditions.checkState(node.isTextual(), "expected a node with a time: <%s>", node);
		return DateTime.parse(node.textValue()).withZone(zone);
	}

	private Duration durationValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		return Duration.standardMinutes(Math.round(node.doubleValue()));
	}

	private static void addLocationValue(Event event, JsonNode latNode, JsonNode lonNode) {
		Location location = locationValue(latNode, lonNode);
		if (location != null) {
			event.addValue(Event.LOCATION, location);
		}
	}

	private static Location locationValue(JsonNode latNode, JsonNode lonNode) {
		if (latNode.isMissingNode() || latNode.isNull()) {
			return null;
		}
		Preconditions.checkState(latNode.isNumber(), "expected a numeric latitude in <%s>", latNode);
		Preconditions.checkState(lonNode.isNumber(), "expected a numeric longitude in <%s>", lonNode);
		return new Location(latNode.decimalValue(), lonNode.decimalValue());
	}

	private static Rating ratingValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		return Rating.valueOf(Math.max(0, Math.min(100, node.intValue())));
	}

	private DecimalMeasure<Length> distanceValue(JsonNode node) {
		return node.isNumber() ? settings.newDistance(node.decimalValue()) : null;
	}

	private DecimalMeasure<Volume> volumeValue(JsonNode node) {
		return node.isNumber() ? settings.newVolume(node.decimalValue()) : null;
	}

	private DecimalMeasure<LengthPerVolume> fuelEfficiencyValue(JsonNode node) {
		return node.isNumber() ? settings.newFuelEfficiency(node.decimalValue()) : null;
	}

	private static UUID uuidValue(JsonNode node) {
		return node.isTextual() ? UUID.fromString(node.textValue()) : null;
	}
}
