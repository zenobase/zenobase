package com.zenobase.tasks.fitbit;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;

import com.zenobase.common.DateTimeZones;
import com.zenobase.common.Measures;
import com.zenobase.common.Pace;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;

abstract class FitbitResultSupport {

	public static final Resource SOURCE = new Resource("Fitbit", "http://fitbit.com/");

	protected final JsonNode node;
	protected final String tag;
	protected final Identity author;
	protected final DateTimeZone timezone;

	protected FitbitResultSupport(JsonNode node, String tag, Identity author, DateTimeZone timezone) {
		this.node = node;
		this.tag = tag;
		this.author = author;
		this.timezone = timezone;
	}

	public abstract List<Event> getEvents();

	protected DateTime dateTimeValue(JsonNode item) {
		return DateTimeZones.toDateTime(LocalDateTime.parse(item.textValue()), timezone);
	}

	protected static Duration durationValue(JsonNode node) {
		return !isZero(node) ? Duration.millis(node.longValue()) : null;
	}

	protected static DecimalMeasure<Length> lengthValue(JsonNode node, Unit<Length> unit) {
		return !isZero(node) ? DecimalMeasure.valueOf(node.decimalValue(), unit) : null;
	}

	protected static DecimalMeasure<Mass> weightValue(JsonNode node, Unit<Mass> unit) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue(), unit) : null;
	}

	protected static DecimalMeasure<Energy> energyValue(JsonNode node, Unit<Energy> unit) {
		BigDecimal value = null;
		if (node.isNumber()) {
			value = node.decimalValue();
		} else if (node.asInt() != 0) {
			value = new BigDecimal(node.asInt());
		}
		return value != null ? DecimalMeasure.valueOf(value, unit) : null;
	}

	protected static Rating ratingValue(JsonNode node) {
		return !isZero(node) ? Rating.valueOf(node.intValue()) : null;
	}

	protected static Percentage percentageValue(JsonNode node) {
		return !isZero(node) ? Percentage.valueOf(node.intValue()) : null;
	}

	protected static Integer countValue(JsonNode node) {
		return !isZero(node) ? Integer.valueOf(node.intValue()) : null;
	}

	protected static DecimalMeasure<Frequency> frequencyValue(JsonNode node) {
		return node.asInt() > 0 ? Measures.valueOf(BigDecimal.valueOf(node.asInt()), Units.BPM) : null;
	}

	protected static DecimalMeasure<Velocity> velocityValue(JsonNode node, Unit<Velocity> unit) {
		return node.asInt() > 0 ? Measures.valueOf(Measures.round(node.decimalValue()), unit) : null;
	}

	protected static DecimalMeasure<Pace> paceValue(JsonNode node, Unit<Pace> unit) {
		return node.asInt() > 0 ? Measures.valueOf(BigDecimal.valueOf(node.asInt()), unit) : null;
	}

	private static boolean isZero(JsonNode node) {
		Preconditions.checkArgument(node.isMissingNode() || node.isNull() || node.isNumber());
		return node.doubleValue() == 0.0;
	}
}
