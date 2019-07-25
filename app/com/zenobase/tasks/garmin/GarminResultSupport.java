package com.zenobase.tasks.garmin;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;

class GarminResultSupport {

	public static final Resource SOURCE = new Resource("Garmin", "https://www.garmin.com/");

	protected final JsonNode node;
	protected final Identity author;

	public GarminResultSupport(JsonNode node, Identity author) {
		this.node = Preconditions.checkNotNull(node);
		this.author = Preconditions.checkNotNull(author);
	}

	protected DateTime startTimeValue(JsonNode node) {
		JsonNode timeNode = node.path("startTimeInSeconds");
		JsonNode zoneNode = node.path("startTimeOffsetInSeconds");
		DateTimeZone zone = DateTimeZone.forOffsetMillis(zoneNode.intValue() * 1000);
		return !isZero(timeNode) ? new DateTime(timeNode.longValue() * 1000, zone) : null;
	}

	protected Duration durationValue(JsonNode node) {
		return !isZero(node) ? Duration.standardSeconds(node.longValue()) : null;
	}

	protected DecimalMeasure<Energy> energyValue(JsonNode node) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue(), Units.KCAL) : null;
	}

	protected DecimalMeasure<Frequency> frequencyValue(JsonNode node) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue(), Units.BPM) : null;
	}

	protected Integer intValue(JsonNode node) {
		return !isZero(node) ? node.intValue() : null;
	}

	protected DecimalMeasure<Length> lengthValue(JsonNode node) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue().setScale(0, RoundingMode.HALF_UP), Units.M) : null;
	}

	protected Location locationValue(JsonNode latitudeNode, JsonNode longitudeNode) {
		BigDecimal latitude = decimalValue(latitudeNode, 7);
		BigDecimal longitude = decimalValue(longitudeNode, 7);
		return latitude != null && longitude != null ? new Location(latitude, longitude) : null;
	}

	private static BigDecimal decimalValue(JsonNode node, int scale) {
		return !isZero(node) ? node.decimalValue().setScale(scale, RoundingMode.HALF_UP) : null;
	}

	private static boolean isZero(JsonNode node) {
		Preconditions.checkArgument(node.isMissingNode() || node.isNull() || node.isNumber());
		return node.doubleValue() == 0.0;
	}
}
