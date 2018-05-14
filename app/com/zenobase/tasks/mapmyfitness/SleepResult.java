package com.zenobase.tasks.mapmyfitness;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Resource;

class SleepResult {

	static final Resource SOURCE = new Resource("MapMyFitness", "https://www.mapmyfitness.com/");

	private final JsonNode node;
	private final Identity author;
	private final String tag;

	public SleepResult(JsonNode node, Identity author, String tag) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.tag = tag;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode sleepNode : node.path("_embedded").path("sleeps")) {
			Event event = newEvent(sleepNode);
			if (event != null) {
				events.add(event);
			}
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		DateTimeZone zone = dateTimeZoneValue(node.path("start_datetime_timezone"));
		DateTime begin = dateTimeValue(node.path("start_datetime_utc"), zone);
		Event event = new Event();
		DateTime end = dateTimeValue(node.path("end_datetime_utc"), zone);
		event.addValue(Event.TAG, tag);
		event.addValue(Event.TIMESTAMP, begin);
		event.addValue(Event.TIMESTAMP, end);
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("aggregates").path("sum"), node.path("aggregates").path("details").path("awake").path("sum")));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private DateTimeZone dateTimeZoneValue(JsonNode node) {
		String value = node.textValue();
		Preconditions.checkNotNull(value, "Can't find start time zone: %s", this.node);
		return DateTimeZone.forID(value);
	}

	private DateTime dateTimeValue(JsonNode node, DateTimeZone zone) {
		String value = node.textValue();
		Preconditions.checkNotNull(value, "Can't find start time: %s", this.node);
		return DateTime.parse(value).withZone(zone);
	}

	private Percentage percentageValue(JsonNode dividendNode, JsonNode divisorNode) {
		int dividend = dividendNode.intValue();
		int divisor = divisorNode.intValue();
		Preconditions.checkState(dividend != 0);
		return Percentage.valueOf(divisor > 0 ? 100 * (dividend - divisor) / dividend : 100);
	}

	public String getNext() {
		return node.path("_links").path("next").path(0).path("href").textValue();
	}
}
