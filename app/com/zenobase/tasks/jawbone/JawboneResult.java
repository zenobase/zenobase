package com.zenobase.tasks.jawbone;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Percentage;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;

abstract class JawboneResult {

	public static final Resource SOURCE = new Resource("Jawbone", "https://jawbone.com/up/");

	protected final JsonNode node;
	protected final Identity author;
	protected final String tag;

	protected JawboneResult(JsonNode node, Identity author, String tag) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.tag = tag;
	}

	public String next() {
		return node.path("links").path("next").textValue();
	}

	protected DateTimeZone dateTimeZoneValue(JsonNode node) {
		DateTimeZone zone = null;
		if (node.isNull()) {
			zone = DateTimeZone.UTC;
		} else if (node.asInt() != 0) {
			zone = DateTimeZone.forOffsetMillis(node.asInt() * 1000);
		} else if (node.isTextual()) {
			zone = DateTimeZone.forID(node.textValue().replace("GMT", ""));
		} else {
			throw new IllegalStateException("expected a node with a time zone: <" + node + ">");
		}
		return zone;
	}

	protected DateTime dateTimeValue(JsonNode node, DateTimeZone zone) {
		Preconditions.checkState(node.isInt(), "expected a node with an epoch time: <%s>", node);
		return new DateTime(node.longValue() * 1000, zone);
	}

	protected Duration durationValue(JsonNode node) {
		return node.isNumber() ? Duration.standardSeconds(node.intValue()) : null;
	}

	protected Rating ratingValue(JsonNode node) {
		int value = node.asInt();
		return value != 0 ? Rating.valueOf(value) : null;
	}

	protected Percentage percentageValue(JsonNode node) {
		int value = node.asInt();
		return value != 0 ? Percentage.valueOf(value) : null;
	}

	protected Location locationValue(JsonNode node) {
		JsonNode latNode = node.path("place_lat");
		JsonNode lonNode = node.path("place_lon");
		if (latNode.isNumber() && lonNode.doubleValue() != 0.0) {
			return new Location(latNode.decimalValue(), lonNode.decimalValue());
		}
		if (latNode.isTextual() && !"".equals(latNode.textValue()) && !"0.0".equals(latNode.textValue())) {
			return new Location(latNode.textValue(), lonNode.textValue());
		}
		return null;
	}

	protected DecimalMeasure<Length> distanceValue(JsonNode node, Unit<Length> unit) {
		return node.isNumber() ? Measures.valueOf(Measures.convert(node.doubleValue(), unit), unit) : null;
	}

	protected DecimalMeasure<Energy> energyValue(JsonNode node) {
		return node.isNumber() ? Measures.<Energy>valueOf(node.decimalValue(), Units.KCAL) : null;
	}

	protected DecimalMeasure<Mass> weightValue(JsonNode node, Unit<Mass> unit) {
		return node.isNumber() ? Measures.valueOf(Measures.convert(node.doubleValue(), unit), unit) : null;
	}
}
