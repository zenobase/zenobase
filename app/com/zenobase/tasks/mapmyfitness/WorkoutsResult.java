package com.zenobase.tasks.mapmyfitness;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class WorkoutsResult {

	private final JsonNode node;
	private final Identity author;
	private final boolean imperial;

	public WorkoutsResult(JsonNode node, Identity author, boolean imperial) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.imperial = imperial;
	}

	public List<Workout> getWorkouts() {
		List<Workout> workouts = Lists.newArrayList();
		for (JsonNode workoutNode : node.path("_embedded").path("workouts")) {
			workouts.add(newWorkout(workoutNode));
		}
		return workouts;
	}

	private Workout newWorkout(JsonNode node) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, dateTimeValue(node.path("start_datetime"), dateTimeZoneValue(node.path("start_locale_timezone"))));
		event.setValue(Event.DURATION, durationValue(node.path("aggregates").path("elapsed_time_total")));
		event.setValue(Event.COUNT, countValue(node.path("aggregates").path("steps_total")));
		event.setValue(Event.DISTANCE, distanceValue(node.path("aggregates").path("distance_total")));
		event.setValue(Event.FREQUENCY, frequencyValue(node.path("aggregates").path("heartrate_avg")));
		event.setValue(Event.SOURCE, resourceValue(node.path("_links").path("self").path(0).path("id")));
		event.setValue(Event.AUTHOR, author);
		String typeId = typeValue(node.path("_links").path("activity_type").path(0).path("id"));
		String routeId = routeValue(node.path("_links").path("route").path(0).path("id"));
		return new Workout(event, typeId, routeId);
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

	private Duration durationValue(JsonNode node) {
		long value = node.asLong();
		Preconditions.checkArgument(value != 0, "Can't find elapsed time: %s", this.node);
		return Duration.standardSeconds(value);
	}

	private Integer countValue(JsonNode node) {
		int value = node.intValue();
		return value > 0 ? value : null;
	}

	private DecimalMeasure<Length> distanceValue(JsonNode node) {
		Unit<Length> unit = imperial ? NonSI.MILE : SI.KILO(SI.METER);
		return node.isNumber() ? Measures.valueOf(Measures.convert(node.doubleValue(), unit), unit) : null;
	}

	private DecimalMeasure<Frequency> frequencyValue(JsonNode node) {
		int value = node.intValue();
		return value > 0 ? Measures.<Frequency>valueOf(BigDecimal.valueOf(value), "bpm") : null;
	}

	private Resource resourceValue(JsonNode node) {
		String value = node.textValue();
		Preconditions.checkNotNull(value, "Can't find workout id: %s", this.node);
		return new Resource("MapMyFitness", "http://www.mapmyfitness.com/workout/" + value);
	}

	private String typeValue(JsonNode node) {
		String value = node.textValue();
		Preconditions.checkNotNull(value, "Can't find activity type: %s", this.node);
		return value;
	}

	private String routeValue(JsonNode node) {
		String value = node.textValue();
		Preconditions.checkNotNull(value, "Can't find route id: %s", this.node);
		return value;
	}

	public String getNext() {
		return node.path("_links").path("next").path(0).path("href").textValue();
	}
}
