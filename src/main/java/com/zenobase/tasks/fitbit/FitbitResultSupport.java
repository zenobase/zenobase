package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.zenobase.common.DateTimeZones;
import com.zenobase.common.Measures;
import com.zenobase.common.Pace;
import com.zenobase.common.Units;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;
import java.math.BigDecimal;
import java.util.Objects;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

abstract class FitbitResultSupport {

	public static final Resource SOURCE = new Resource("Fitbit", "https://fitbit.com/");

	protected final JsonNode node;
	protected final @Nullable String tag;
	protected final Identity author;
	protected final @Nullable DateTimeZone timezone;

	protected FitbitResultSupport(
		JsonNode node,
		@Nullable String tag,
		Identity author,
		@Nullable DateTimeZone timezone
	) {
		this.node = node;
		this.tag = tag;
		this.author = author;
		this.timezone = timezone;
	}

	protected DateTime dateTimeValue(JsonNode item) {
		return DateTimeZones.toDateTime(LocalDateTime.parse(item.textValue()), timezone);
	}

	protected static @Nullable Duration durationValue(JsonNode node) {
		return !isZero(node) ? Duration.millis(node.longValue()) : null;
	}

	protected static @Nullable DecimalMeasure<Length> lengthValue(JsonNode node, Unit<Length> unit) {
		return !isZero(node) ? DecimalMeasure.valueOf(node.decimalValue(), unit) : null;
	}

	protected static @Nullable DecimalMeasure<Mass> weightValue(JsonNode node, Unit<Mass> unit) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue(), unit) : null;
	}

	protected static @Nullable DecimalMeasure<Energy> energyValue(JsonNode node, Unit<Energy> unit) {
		BigDecimal value = null;
		if (node.isNumber()) {
			value = node.decimalValue();
		} else if (node.asInt() != 0) {
			value = new BigDecimal(node.asInt());
		}
		return value != null ? DecimalMeasure.valueOf(value, unit) : null;
	}

	protected static @Nullable Rating ratingValue(JsonNode node) {
		return !isZero(node) ? Rating.valueOf(node.intValue()) : null;
	}

	protected static @Nullable Percentage percentageValue(JsonNode node) {
		return !isZero(node) ? Percentage.valueOf(node.intValue()) : null;
	}

	protected static @Nullable Integer countValue(JsonNode node) {
		return !isZero(node) ? node.intValue() : null;
	}

	protected static @Nullable DecimalMeasure<Frequency> frequencyValue(JsonNode node) {
		return node.asInt() > 0 ? Measures.valueOf(BigDecimal.valueOf(node.asInt()), Units.BPM) : null;
	}

	protected static @Nullable DecimalMeasure<Velocity> velocityValue(JsonNode node, Unit<Velocity> unit) {
		return node.asInt() > 0
			? Measures.valueOf(Objects.requireNonNull(Measures.round(node.decimalValue())), unit)
			: null;
	}

	protected static @Nullable DecimalMeasure<Pace> paceValue(JsonNode node, Unit<Pace> unit) {
		return node.asInt() > 0 ? Measures.valueOf(BigDecimal.valueOf(node.asInt()), unit) : null;
	}

	private static boolean isZero(JsonNode node) {
		Preconditions.checkArgument(node.isMissingNode() || node.isNull() || node.isNumber());
		return node.doubleValue() == 0.0;
	}
}
