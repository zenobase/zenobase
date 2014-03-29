package com.zenobase.tasks.strava;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;

class ActivitiesResult {

	private static final Unit<Frequency> UNIT_BPM = Measures.<Frequency>parseUnit("bpm");

	private final JsonNode node;
	private final Identity author;
	private final boolean metric;

	public ActivitiesResult(JsonNode node, Identity author, boolean metric) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.metric = metric;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode activityNode : node) {
			events.add(newEvent(activityNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = new Event();
		event.addValue(Event.TAG, node.path("type").textValue());
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("start_date"), dateTimeZoneValue(node.path("timezone"))));
		event.setValue(Event.DURATION, durationValue(node.path("elapsed_time")));
		event.setValue(Event.LOCATION, locationValue(node.path("start_latlng")));
		event.setValue(Event.DISTANCE, distanceValue(node.path("distance"), metric ? SI.KILOMETER : NonSI.MILE));
		event.setValue(Event.HEIGHT, distanceValue(node.path("total_elevation_gain"), metric ? SI.METER : NonSI.FOOT));
		event.setValue(Event.ENERGY, energyValue(node.path("kilojoules")));
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

	private Duration durationValue(JsonNode node) {
		return node.isNumber() ? Duration.standardSeconds(node.intValue()) : null;
	}

	private Location locationValue(JsonNode node) {
		if (node.isMissingNode()) {
			return null;
		}
		Preconditions.checkState(node.size() == 2, "expected a node with a latitude and a longitude: <%s>", node);
		Preconditions.checkState(node.get(0).isNumber(), "expected a numeric latitude in <%s>", node);
		Preconditions.checkState(node.get(1).isNumber(), "expected a numeric longitude in <%s>", node);
		return new Location(node.get(0).decimalValue(), node.get(1).decimalValue());
	}

	private DecimalMeasure<Length> distanceValue(JsonNode node, Unit<Length> unit) {
		return node.isNumber() ? Measures.valueOf(Measures.convert(node.doubleValue(), unit), unit) : null;
	}

	private DecimalMeasure<Energy> energyValue(JsonNode node) {
		return node.isNumber() ? Measures.<Energy>valueOf(node.decimalValue(), SI.KILO(SI.JOULE)) : null;
	}

	private DecimalMeasure<Frequency> frequencyValue(JsonNode node) {
		return node.isNumber() ? Measures.<Frequency>valueOf(node.decimalValue(), UNIT_BPM) : null;
	}

	private Resource resourceValue(JsonNode node) {
		return node.isNumber() ? new Resource("Strava", "http://www.strava.com/activities/" + node.intValue()) : null;
	}
}
