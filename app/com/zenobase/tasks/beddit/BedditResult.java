package com.zenobase.tasks.beddit;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Frequency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Resource;

public class BedditResult {

	public static final Resource SOURCE = new Resource("Beddit", "https://www.beddit.com/");

	private final String tag;
	private final Identity author;
	private final DateTime from;
	private final ArrayNode node;

	public BedditResult(String tag, Identity author, DateTime from, ArrayNode node) {
		this.tag = tag;
		this.author = author;
		this.from = from;
		this.node = node;
	}

	public String getCursor() {
		return node.path("cursor").textValue();
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode sleepNode : node) {
			addSleep(sleepNode, events);
		}
		return events;
	}

	private void addSleep(JsonNode node, List<Event> events) {
		DateTimeZone zone = dateTimeZoneValue(node.path("timezone"));
		DateTime begin = dateTimeValue(node.path("start_timestamp"), zone);
		DateTime end = dateTimeValue(node.path("end_timestamp"), zone);
		if (!begin.isBefore(from)) {
			Event event = new Event();
			event.addValue(Event.TAG, tag);
			event.setValue(Event.SOURCE, SOURCE);
			event.setValue(Event.AUTHOR, author);
			event.addValue(Event.TIMESTAMP, begin);
			event.addValue(Event.TIMESTAMP, end);
			event.setValue(Event.DURATION, new Duration(begin, end));
			event.setValue(Event.FREQUENCY, frequencyValue(node.path("properties").path("resting_heart_rate")));
			event.setValue(Event.PERCENTAGE, percentageValue(stageDuration(node, 'S') + stageDuration(node, 'R'), stageDuration(node, 'W')));
			events.add(event);
		}
	}

	private static DateTimeZone dateTimeZoneValue(JsonNode node) {
		return node.isMissingNode() ? DateTimeZone.UTC : DateTimeZone.forID(node.textValue());
	}

	private static DateTime dateTimeValue(JsonNode node, DateTimeZone zone) {
		long value = Math.round(node.doubleValue() * 1000L);
		Preconditions.checkArgument(value != 0L, "Can't find timestamp: %s", node);
		return new DateTime(value, zone);
	}

	private static DecimalMeasure<Frequency> frequencyValue(JsonNode node) {
		return node.isBigDecimal() ? Measures.valueOf(node.decimalValue().setScale(0, BigDecimal.ROUND_HALF_UP), Units.BPM) : null;
	}

	private static int stageDuration(JsonNode node, char stage) {
		return node.path("properties").path("stage_duration_" + stage).intValue();
	}

	private static Percentage percentageValue(int a, int b) {
		return a + b > 0 ? Percentage.valueOf(Ints.checkedCast(100 * a / (a + b))) : null;
	}
}
