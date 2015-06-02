package com.zenobase.tasks.automatic;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.quantity.Volume;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import com.zenobase.common.LengthPerVolume;
import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;

class TripsResult {

	static final Resource SOURCE = new Resource("Automatic", "https://www.automatic.com/");

	private final JsonNode node;
	private final Identity author;
	private final String tag;
	private final boolean metric;

	public TripsResult(JsonNode node, Identity author, String tag, boolean metric) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.tag = tag;
		this.metric = metric;
	}

	public boolean hasNext() {
		return node.path("_metadata").path("next").isTextual();
	}

	public List<Trip> getTrips() {
		List<Trip> events = Lists.newArrayList();
		for (JsonNode tripNode : node.path("results")) {
			events.add(newTrip(tripNode));
		}
		return events;
	}

	private Trip newTrip(JsonNode node) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("started_at"), dateTimeZoneValue(node.path("start_timezone"))));
		event.setValue(Event.DURATION, durationValue(node.path("duration_s")));
		event.addValue(Event.TAG, tag);
		for (JsonNode tagNode : node.path("tags")) {
			event.addValue(Event.TAG, tagNode.textValue());
		}
		addLocationValue(event, node.path("start_location"));
		addLocationValue(event, node.path("end_location"));
		event.setValue(Event.RATING, ratingValue(node.path("score_events"), node.path("score_speeding")));
		event.setValue(Event.CURRENCY, Measures.round(decimalValue(node.path("fuel_cost_usd"))));
		event.setValue(Event.DISTANCE, Measures.round(distanceValue(node.path("distance_m"))));
		event.setValue(Event.VOLUME, Measures.round(volumeValue(node.path("fuel_volume_l"))));
		event.setValue(Event.DISTANCE_PER_VOLUME, Measures.round(distancePerVolumeValue(node.path("average_kmpl"))));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return new Trip(event, idValue(node.path("vehicle")));
	}

	private static DateTimeZone dateTimeZoneValue(JsonNode node) {
		return node.isTextual() && node.textValue().indexOf(' ') == -1 // handle "No Time Zone Available"
			? DateTimeZone.forID(node.textValue())
			: DateTimeZone.UTC;
	}

	private static DateTime dateTimeValue(JsonNode node, DateTimeZone zone) {
		Preconditions.checkState(node.isTextual(), "expected a node with a time: <%s>", node);
		return DateTime.parse(node.textValue()).withZone(zone);
	}

	private Duration durationValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		return Duration.standardSeconds(node.intValue());
	}

	private static void addLocationValue(Event event, JsonNode node) {
		Location location = locationValue(node);
		if (location != null) {
			event.addValue(Event.LOCATION, location);
		}
	}

	private static Location locationValue(JsonNode node) {
		if (node.isMissingNode() || node.isNull()) {
			return null;
		}
		Preconditions.checkState(node.path("lat").isNumber(), "expected a numeric latitude in <%s>", node);
		Preconditions.checkState(node.path("lon").isNumber(), "expected a numeric longitude in <%s>", node);
		return new Location(node.path("lat").decimalValue(), node.path("lon").decimalValue());
	}

	private Rating ratingValue(JsonNode... nodes) {
		double score = 0.0;
		for (JsonNode scoreNode : nodes) {
			if (node.doubleValue() < 0) {
				System.err.println("negative score: " + node);
			}
			score += scoreNode.doubleValue();
		}
		return Rating.valueOf(Math.min(100, Math.max(0, Ints.checkedCast(Math.round(score)))));
	}

	private DecimalMeasure<Length> distanceValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		DecimalMeasure<Length> value = Measures.valueOf(node.decimalValue(), Units.M);
		return value.to(metric ? Units.KM : Units.MI, MathContext.DECIMAL32);
	}

	private DecimalMeasure<Volume> volumeValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		DecimalMeasure<Volume> value = Measures.valueOf(node.decimalValue(), Units.L);
		if (!metric) {
			value = value.to(Units.GAL, MathContext.DECIMAL32);
		}
		return value;
	}

	private DecimalMeasure<LengthPerVolume> distancePerVolumeValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		DecimalMeasure<LengthPerVolume> value = Measures.valueOf(node.decimalValue(), Units.KPL);
		if (!metric) {
			value = value.to(Units.MPG, MathContext.DECIMAL32);
		}
		return value;
	}

	private static BigDecimal decimalValue(JsonNode node) {
		return node.isNumber() ? node.decimalValue() : null;
	}

	private static String idValue(JsonNode node) {
		return node.isTextual() && node.textValue().indexOf(' ') == -1 // handle "No CarID Available"
			? node.textValue()
			: null;
	}
}
