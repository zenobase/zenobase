package com.zenobase.tasks.fitbit;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;

import com.zenobase.common.DateTimeZones;
import com.zenobase.common.Measures;
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
		long value = node.longValue();
		return value > 0 ? Duration.millis(value) : null;
	}

	protected static DecimalMeasure<Length> lengthValue(JsonNode node, Unit<Length> unit) {
		BigDecimal value = node.decimalValue();
		return value != null ? DecimalMeasure.valueOf(value, unit) : null;
	}

	protected static DecimalMeasure<Mass> weightValue(JsonNode node, Unit<Mass> unit) {
		int value = node.intValue();
		return value > 0 ? Measures.valueOf(node.decimalValue(), unit) : null;
	}

	protected static DecimalMeasure<Energy> energyValue(JsonNode node, Unit<Energy> unit) {
		BigDecimal value = node.decimalValue();
		if (BigDecimal.ZERO.equals(value) && node.asInt() != 0) {
			value = new BigDecimal(node.asInt());
		}
		return value != null ? DecimalMeasure.valueOf(value, unit) : null;
	}

	protected static Rating ratingValue(JsonNode node) {
		int value = node.intValue();
		return value > 0 ? Rating.valueOf(value) : null;
	}

	protected static Percentage percentageValue(JsonNode node) {
		int value = node.intValue();
		return value > 0 ? Percentage.valueOf(value) : null;
	}
}
