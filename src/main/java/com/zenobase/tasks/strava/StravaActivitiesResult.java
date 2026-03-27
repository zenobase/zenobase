package com.zenobase.tasks.strava;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.Measures;
import com.zenobase.common.Pace;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;

class StravaActivitiesResult {

	private final JsonNode node;
	private final Identity author;
	private final boolean metric;

	public StravaActivitiesResult(JsonNode node, Identity author, boolean metric) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.metric = metric;
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode activityNode : node) {
			events.add(newEvent(activityNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		var event = new Event();
		event.addValue(Event.TAG, node.path("type").textValue());
		event.setValue(
				Event.TIMESTAMP, dateTimeValue(node.path("start_date"), dateTimeZoneValue(node.path("timezone"))));
		event.setValue(Event.DURATION, durationValue(node.path("elapsed_time")));
		event.setValue(Event.LOCATION, locationValue(node.path("start_latlng")));
		event.setValue(Event.DISTANCE, distanceValue(node.path("distance"), metric ? Units.KM : Units.MI, 1));
		event.setValue(Event.HEIGHT, distanceValue(node.path("total_elevation_gain"), metric ? Units.M : Units.FT, 0));
		event.setValue(Event.ENERGY, energyValue(node.path("kilojoules")));
		event.setValue(Event.VELOCITY, velocityValue(node.path("average_speed"), metric ? Units.KMH : Units.MPH));
		event.setValue(Event.PACE, paceValue(node.path("average_speed"), metric ? Units.S_PER_KM : Units.S_PER_MI));
		event.setValue(Event.FREQUENCY, frequencyValue(node.path("average_heartrate")));
		event.setValue(Event.SOURCE, resourceValue(node.path("id")));
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private DateTimeZone dateTimeZoneValue(JsonNode node) {
		Preconditions.checkState(node.isTextual(), "expected a node with a time zone: <%s>", node);
		String[] tokens = node.textValue().split(" ");
		Preconditions.checkState(tokens.length == 2, "can't parse time zone value: <%s>", node);
		return DateTimeZone.forID(tokens[1]);
	}

	private DateTime dateTimeValue(JsonNode node, DateTimeZone zone) {
		Preconditions.checkState(node.isTextual(), "expected a node with a time: <%s>", node);
		return DateTime.parse(node.textValue()).withZone(zone);
	}

	private @Nullable Duration durationValue(JsonNode node) {
		return node.isNumber() ? Duration.standardSeconds(node.intValue()) : null;
	}

	private @Nullable Location locationValue(JsonNode node) {
		if (node.isMissingNode() || node.isNull()) {
			return null;
		}
		Preconditions.checkState(node.size() == 2, "expected a node with a latitude and a longitude: <%s>", node);
		Preconditions.checkState(node.path(0).isNumber(), "expected a numeric latitude in <%s>", node);
		Preconditions.checkState(node.path(1).isNumber(), "expected a numeric longitude in <%s>", node);
		return new Location(node.path(0).decimalValue(), node.path(1).decimalValue());
	}

	private @Nullable DecimalMeasure<Length> distanceValue(JsonNode node, Unit<Length> unit, int scale) {
		return !isZero(node)
				? Measures.valueOf(
						Objects.requireNonNull(Measures.round(Measures.convert(node.doubleValue(), unit), scale)), unit)
				: null;
	}

	private @Nullable DecimalMeasure<Velocity> velocityValue(JsonNode node, Unit<Velocity> unit) {
		return !isZero(node)
				? Measures.valueOf(
						Objects.requireNonNull(Measures.round(Measures.convert(node.doubleValue(), unit), 1)), unit)
				: null;
	}

	private @Nullable DecimalMeasure<Pace> paceValue(JsonNode node, Unit<Pace> unit) {
		return !isZero(node)
				? Measures.valueOf(
						Objects.requireNonNull(
								Measures.round(Measures.convert(Math.pow(node.doubleValue(), -1.0), unit), 0)),
						unit)
				: null;
	}

	private @Nullable DecimalMeasure<Energy> energyValue(JsonNode node) {
		return !isZero(node)
				? Measures.valueOf(Objects.requireNonNull(Measures.round(node.decimalValue(), 0)), Units.KJ)
				: null;
	}

	private @Nullable DecimalMeasure<Frequency> frequencyValue(JsonNode node) {
		return !isZero(node)
				? Measures.valueOf(Objects.requireNonNull(Measures.round(node.decimalValue(), 0)), Units.BPM)
				: null;
	}

	private @Nullable Resource resourceValue(JsonNode node) {
		return !isZero(node) ? new Resource("Strava", "https://www.strava.com/activities/" + node.intValue()) : null;
	}

	private static boolean isZero(JsonNode node) {
		Preconditions.checkArgument(node.isMissingNode() || node.isNull() || node.isNumber());
		return node.doubleValue() == 0.0;
	}
}
