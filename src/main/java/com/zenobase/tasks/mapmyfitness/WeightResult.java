package com.zenobase.tasks.mapmyfitness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.measure.DecimalMeasure;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Resource;

class WeightResult {

	static final Resource SOURCE = new Resource("MapMyFitness", "https://www.mapmyfitness.com/");

	private final JsonNode node;
	private final Identity author;
	private final @Nullable String tag;
	private final boolean imperial;

	public WeightResult(JsonNode node, Identity author, @Nullable String tag, boolean imperial) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.tag = tag;
		this.imperial = imperial;
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode bodymassNode : node.path("_embedded").path("bodymasses")) {
			events.add(newEvent(bodymassNode));
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		var event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValue(
			Event.TIMESTAMP,
			dateTimeValue(node.path("datetime_utc"), dateTimeZoneValue(node.path("datetime_timezone")))
		);
		event.setValue(Event.WEIGHT, weightValue(node.path("mass")));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("fat_percent")));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private DateTimeZone dateTimeZoneValue(JsonNode node) {
		String value = node.textValue();
		Preconditions.checkNotNull(value, "Can't find time zone: %s", this.node);
		return DateTimeZone.forID(value);
	}

	private DateTime dateTimeValue(JsonNode node, DateTimeZone zone) {
		String value = node.textValue();
		Preconditions.checkNotNull(value, "Can't find start time: %s", this.node);
		return DateTime.parse(value).withZone(zone);
	}

	private @Nullable DecimalMeasure<Mass> weightValue(JsonNode node) {
		Unit<Mass> unit = imperial ? Units.LB : Units.KG;
		return !isZero(node)
			? Measures.valueOf(Objects.requireNonNull(Measures.round(Measures.convert(node.asDouble(), unit), 1)), unit)
			: null;
	}

	private @Nullable Percentage percentageValue(JsonNode node) {
		return !isZero(node) ? Percentage.valueOf(BigDecimal.valueOf(node.asDouble())) : null;
	}

	public String getNext() {
		return node.path("_links").path("next").path(0).path("href").textValue();
	}

	private static boolean isZero(JsonNode node) {
		return node.isNull() || node.asDouble() == 0.0;
	}
}
