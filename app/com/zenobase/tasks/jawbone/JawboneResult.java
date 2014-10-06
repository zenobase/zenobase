package com.zenobase.tasks.jawbone;

import java.math.RoundingMode;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Quantity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

import com.zenobase.common.Measures;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
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
		Preconditions.checkState(node.isTextual(), "expected a node with a time zone: <%s>", node);
		int offset = node.asInt();
		return offset != 0 ? DateTimeZone.forOffsetMillis(offset * 1000) : DateTimeZone.forID(node.textValue().replace("GMT", ""));
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

	protected Location locationValue(JsonNode node) {
		JsonNode latNode = node.path("place_lat");
		JsonNode lonNode = node.path("place_lon");
		if (!latNode.isTextual() || "".equals(latNode.textValue())) {
			return null;
		}
		return new Location(latNode.textValue(), lonNode.textValue());
	}

	protected DecimalMeasure<Length> distanceValue(JsonNode node, Unit<Length> unit) {
		return node.isNumber() ? Measures.valueOf(Measures.convert(node.doubleValue(), unit), unit) : null;
	}

	protected <T extends Quantity> DecimalMeasure<T> round(DecimalMeasure<T> value) {
		return value != null ? DecimalMeasure.valueOf(value.getValue().setScale(0, RoundingMode.HALF_UP), value.getUnit()) : null;
	}

	protected DecimalMeasure<Energy> energyValue(JsonNode node) {
		Unit<Energy> unit = Measures.parseUnit("cal");
		return node.isNumber() ? Measures.<Energy>valueOf(node.decimalValue(), SI.KILO(unit)) : null;
	}
}
