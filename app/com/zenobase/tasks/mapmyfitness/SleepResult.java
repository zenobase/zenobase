package com.zenobase.tasks.mapmyfitness;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Resource;

class SleepResult {

	static final Resource SOURCE = new Resource("MapMyFitness", "http://www.mapmyfitness.com/");

	private final JsonNode node;
	private final Identity author;
	private final DateTime noEarlierThan;

	public SleepResult(JsonNode node, Identity author, DateTime noEarlierThan) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.noEarlierThan = noEarlierThan;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode actigraphyNode : node.path("_embedded").path("actigraphies")) {
			RangeMap<DateTime, DateTimeZone> zones = getZoneMap(actigraphyNode.path("timezones"));
			for (JsonNode sleepNode : actigraphyNode.path("metrics").path("sleep")) {
				Event event = newEvent(sleepNode, zones);
				if (event != null) {
					events.add(event);
				}
			}
		}
		return events;
	}

	private Event newEvent(JsonNode node, RangeMap<DateTime, DateTimeZone> zones) {
		DateTime begin = dateTimeValue(node.path("start_datetime_utc"), zones);
		if (begin.isBefore(noEarlierThan)) {
			return null;
		}
		Event event = new Event();
		DateTime end = dateTimeValue(node.path("end_datetime_utc"), zones);
		event.addValue(Event.TIMESTAMP, begin);
		event.addValue(Event.TIMESTAMP, end);
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.setValue(Event.PERCENTAGE, percentageValue(node.path("aggregates").path("sum"), node.path("aggregates").path("details").path("awake").path("sum")));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private static RangeMap<DateTime, DateTimeZone> getZoneMap(JsonNode node) {
		RangeMap<DateTime, DateTimeZone> zones = TreeRangeMap.create();
		if (node.size() > 0) {
			DateTime t0 = new DateTime(0);
			DateTimeZone zone = null;
			for (JsonNode zoneNode : node) {
				DateTime t1 = new DateTime(zoneNode.path(0).longValue() * 1000, DateTimeZone.UTC);
				if (zone != null) {
					zones.put(Range.closedOpen(t0, t1), zone);
				}
				zone = DateTimeZone.forID(zoneNode.path(1).textValue());
				t0 = t1;
			}
			zones.put(Range.atLeast(new DateTime(t0)), zone);
		}
		Preconditions.checkState(!zones.asMapOfRanges().isEmpty(), "Couldn't find any timezone mappings");
		return zones;
	}

	private DateTime dateTimeValue(JsonNode node, RangeMap<DateTime, DateTimeZone> zones) {
		String value = node.textValue();
		Preconditions.checkNotNull(value, "Can't find start time: %s", this.node);
		DateTime t = DateTime.parse(value);
		DateTimeZone zone = zones.get(t);
		Preconditions.checkNotNull(zone, "Can't determine time zone for %s using %s", t, zones);
		return t.withZone(zone);
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
