package com.zenobase.tasks.oura;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;

abstract class OuraResultSupport {

	static final Resource SOURCE = new Resource("Oura", "https://ouraring.com/");

	protected final JsonNode node;
	protected final Identity author;

	protected OuraResultSupport(JsonNode node, Identity author) {
		this.node = Preconditions.checkNotNull(node);
		this.author = Preconditions.checkNotNull(author);
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode eventNode : node.path("data")) {
			Event event = newEvent(eventNode);
			if (event != null) {
				events.add(event);
			}
		}
		return events;
	}

	protected abstract @Nullable Event newEvent(JsonNode node);

	protected DateTime dateTimeValue(JsonNode node) {
		Preconditions.checkState(node.isTextual(), "expected a node with a time: <%s>", node);
		return DateTime.parse(node.textValue());
	}

	protected LocalDate dateValue(JsonNode node) {
		Preconditions.checkState(node.isTextual(), "expected a node with a date: <%s>", node);
		return LocalDate.parse(node.textValue());
	}

	protected @Nullable Duration durationValue(JsonNode node) {
		return !isZero(node) ? Duration.standardSeconds(node.intValue()) : null;
	}

	protected @Nullable Integer intValue(JsonNode node) {
		return !isZero(node) ? node.intValue() : null;
	}

	protected @Nullable DecimalMeasure<Length> distanceValue(JsonNode node) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue(), Units.MI) : null;
	}

	protected @Nullable DecimalMeasure<Energy> energyValue(JsonNode node) {
		return !isZero(node)
				? Measures.valueOf(node.decimalValue().setScale(0, RoundingMode.HALF_UP), Units.KCAL)
				: null;
	}

	protected @Nullable DecimalMeasure<Frequency> frequencyValue(JsonNode node) {
		return !isZero(node)
				? Measures.valueOf(node.decimalValue().setScale(0, RoundingMode.HALF_UP), Units.BPM)
				: null;
	}

	protected @Nullable Rating ratingValue(JsonNode node) {
		return !isZero(node) ? Rating.valueOf(node.intValue()) : null;
	}

	protected @Nullable Percentage percentageValue(JsonNode node) {
		return !isZero(node) ? Percentage.valueOf(node.intValue()) : null;
	}

	private static boolean isZero(JsonNode node) {
		Preconditions.checkArgument(node.isMissingNode() || node.isNull() || node.isNumber());
		return node.doubleValue() == 0.0;
	}
}
