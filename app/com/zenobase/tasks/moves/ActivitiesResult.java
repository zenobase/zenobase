package com.zenobase.tasks.moves;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.elasticsearch.common.base.Strings;
import org.elasticsearch.common.collect.Sets;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.ISODateTimeFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class ActivitiesResult {

	public static final Resource SOURCE = new Resource("Moves", "http://www.moves-app.com/");

	private final JsonNode node;
	private final Identity author;
	private final DateTime begin;
	private final Unit<Length> lengthUnit;
	private final Unit<Energy> energyUnit;

	public ActivitiesResult(JsonNode node, Identity author, DateTime begin, Unit<Length> lengthUnit, Unit<Energy> energyUnit) {
		this.author = Preconditions.checkNotNull(author);
		this.begin = Preconditions.checkNotNull(begin);
		this.lengthUnit = Preconditions.checkNotNull(lengthUnit);
		this.energyUnit = Preconditions.checkNotNull(energyUnit);
		this.node = Preconditions.checkNotNull(node);
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode dayNode : node) {
			addEventsForDay(dayNode, events);
		}
		return events;
	}

	public void addEventsForDay(JsonNode dayNode, List<Event> events) {
		for (JsonNode segmentNode : dayNode.path("segments")) {
			addEventsForSegment(segmentNode, events);
		}
	}

	public void addEventsForSegment(JsonNode segmentNode, List<Event> events) {
		if ("move".equals(segmentNode.path("type").textValue())) {
			for (JsonNode activityNode : segmentNode.path("activities")) {
				Event event = getEvent(activityNode);
				if (event != null) {
					events.add(event);
				}
			}
		}
	}

	public Event getEvent(JsonNode activityNode) {
		Event event = null;
		DateTime begin = dateTimeValue(activityNode.path("startTime"));
		if (!begin.isBefore(this.begin)) {
			event = new Event();
			event.setValue(Event.TIMESTAMP, begin);
			event.setValue(Event.DURATION, Duration.standardSeconds(activityNode.path("duration").intValue()));
			Set<String> tags = Sets.newLinkedHashSet();
			addTextValue(activityNode.path("activity"), tags);
			addTextValue(activityNode.path("group"), tags);
			event.setValues(Event.TAG, tags);
			event.setValue(Event.COUNT, intValue(activityNode.path("steps")));
			event.setValue(Event.ENERGY, measureValue(activityNode.path("calories"), energyUnit));
			event.setValue(Event.DISTANCE, convertValue(activityNode.path("distance"), lengthUnit));
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
		}
		return event;
	}

	private static void addTextValue(JsonNode node, Collection<String> values) {
		String value = node.textValue();
		if (!Strings.isNullOrEmpty(value)) {
			values.add(value);
		}
	}

	private static DateTime dateTimeValue(JsonNode node) {
		return DateTime.parse(node.textValue(), ISODateTimeFormat.basicDateTimeNoMillis().withOffsetParsed());
	}

	private static Integer intValue(JsonNode node) {
		return node.isInt() ? node.intValue() : null;
	}

	private static <Q extends Quantity> DecimalMeasure<Q> measureValue(JsonNode node, Unit<Q> unit) {
		return node.isNumber() ? Measures.<Q>valueOf(node.decimalValue(), unit) : null;
	}

	private static <Q extends Quantity> DecimalMeasure<Q> convertValue(JsonNode node, Unit<Q> unit) {
		return node.isNumber() ? Measures.valueOf(Measures.convert(node.doubleValue(), unit), unit) : null;
	}
}
