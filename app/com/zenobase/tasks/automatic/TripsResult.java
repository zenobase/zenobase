package com.zenobase.tasks.automatic;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.quantity.Volume;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.json.LengthPerVolume;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;

class TripsResult {

	static final Resource SOURCE = new Resource("Automatic", "https://www.automatic.com/");

	private final JsonNode node;
	private final Identity author;
	private final boolean metric;

	public TripsResult(JsonNode node, Identity author, boolean metric) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.metric = metric;
	}

	public List<Trip> getTrips() {
		List<Trip> events = Lists.newArrayList();
		for (JsonNode tripNode : node) {
			events.add(newTrip(tripNode));
		}
		return events;
	}

	private Trip newTrip(JsonNode node) {
		Event event = new Event();
		DateTime begin = dateTimeValue(node.path("start_time"), dateTimeZoneValue(node.path("start_time_zone")));
		DateTime end = dateTimeValue(node.path("end_time"), dateTimeZoneValue(node.path("end_time_zone")));
		event.setValue(Event.TIMESTAMP, begin);
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.addValue(Event.LOCATION, locationValue(node.path("start_location")));
		event.addValue(Event.LOCATION, locationValue(node.path("end_location")));
		event.setValue(Event.CURRENCY, Measures.round(decimalValue(node.path("fuel_cost_usd"))));
		event.setValue(Event.DISTANCE, Measures.round(distanceValue(node.path("distance_m"))));
		event.setValue(Event.VOLUME, Measures.round(volumeValue(node.path("fuel_volume_gal"))));
		event.setValue(Event.DISTANCE_PER_VOLUME, Measures.round(distancePerVolumeValue(node.path("average_mpg"))));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return new Trip(event, node.path("vehicle").path("id").textValue());
	}

	private DateTimeZone dateTimeZoneValue(JsonNode node) {
		Preconditions.checkState(node.isTextual(), "expected a node with a time zone: <%s>", node);
		return DateTimeZone.forID(node.textValue());
	}

	private DateTime dateTimeValue(JsonNode node, DateTimeZone zone) {
		Preconditions.checkState(node.isLong(), "expected a node with a time: <%s>", node);
		return new DateTime(node.longValue(), zone);
	}

	private Location locationValue(JsonNode node) {
		if (node.isMissingNode() || node.isNull()) {
			return null;
		}
		Preconditions.checkState(node.path("lat").isNumber(), "expected a numeric latitude in <%s>", node);
		Preconditions.checkState(node.path("lon").isNumber(), "expected a numeric longitude in <%s>", node);
		return new Location(node.path("lat").decimalValue(), node.path("lon").decimalValue());
	}

	private DecimalMeasure<Length> distanceValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		DecimalMeasure<Length> value = Measures.valueOf(node.decimalValue(), SI.METER);
		return value.to(metric ? SI.KILOMETER : NonSI.MILE, MathContext.DECIMAL32);
	}

	private DecimalMeasure<Volume> volumeValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		DecimalMeasure<Volume> value = Measures.valueOf(node.decimalValue(), NonSI.GALLON_LIQUID_US);
		if (metric) {
			value = value.to(NonSI.LITER, MathContext.DECIMAL32);
		}
		return value;
	}

	private DecimalMeasure<LengthPerVolume> distancePerVolumeValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		DecimalMeasure<LengthPerVolume> value = Measures.valueOf(node.decimalValue(), "mpg");
		if (metric) {
			value = value.to(Measures.<LengthPerVolume>parseUnit("kpl"), MathContext.DECIMAL32);
		}
		return value;
	}

	private BigDecimal decimalValue(JsonNode node) {
		return node.isNumber() ? node.decimalValue() : null;
	}
}
