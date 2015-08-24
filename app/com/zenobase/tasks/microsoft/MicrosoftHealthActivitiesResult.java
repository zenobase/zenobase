package com.zenobase.tasks.microsoft;

import java.math.MathContext;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;

class MicrosoftHealthActivitiesResult extends MicrosoftHealthResultSupport {

	private final boolean metric;

	public MicrosoftHealthActivitiesResult(JsonNode node, Identity author, DateTimeZone zone, boolean metric) {
		super(node, author, zone);
		this.metric = metric;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (String activityType : ImmutableList.of("bikeActivities", "freePlayActivities", "golfActivities", "guidedWorkoutActivities", "runActivities")) {
			for (JsonNode activityNode : node.path(activityType)) {
				events.add(newEvent(activityNode));
			}
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = new Event();
		DateTime begin = dateTimeValue(node.path("startTime"));
		DateTime end = dateTimeValue(node.path("endTime"));
		event.setValue(Event.TIMESTAMP, begin);
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.addValue(Event.TAG, node.path("activityType").textValue());
		for (JsonNode mapPoint : node.path("mapPoints")) {
			Location location = locationValue(mapPoint.path("location"));
			if (location != null) {
				event.setValue(Event.LOCATION, location);
				break;
			}
		}
		event.setValue(Event.DISTANCE, distanceValue(node.path("distanceSummary").path("totalDistance")));
		event.setValue(Event.HEIGHT, heightValue(node.path("distanceSummary").path("elevationGain")));
		event.setValue(Event.FREQUENCY, frequencyValue(node.path("heartRateSummary").path("averageHeartRate")));
		event.setValue(Event.ENERGY, energyValue(node.path("caloriesBurnedSummary").path("totalCalories")));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private DecimalMeasure<Length> distanceValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		DecimalMeasure<Length> value = Measures.valueOf(node.decimalValue(), Units.CM);
		return Measures.round(value.to(metric ? Units.KM : Units.MI, MathContext.DECIMAL32));
	}

	private DecimalMeasure<Length> heightValue(JsonNode node) {
		if (!node.isNumber()) {
			return null;
		}
		DecimalMeasure<Length> value = Measures.valueOf(node.decimalValue(), Units.CM);
		return Measures.round(value.to(metric ? Units.M : Units.FT, MathContext.DECIMAL32), 0);
	}
}
