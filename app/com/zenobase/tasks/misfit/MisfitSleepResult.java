package com.zenobase.tasks.misfit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;

class MisfitSleepResult extends MisfitResultSupport {

	private final String tag;

	public MisfitSleepResult(JsonNode node, Identity author, String tag) {
		super("sleeps", node, author);
		this.tag = Preconditions.checkNotNull(tag);
	}

	@Override
	protected Event newEvent(JsonNode node) {
		Event event = null;
		DateTime t = dateTimeValue(node.path("startTime"));
		Duration d = durationValue(node.path("duration"));
		event = new Event();
		event.addValue(Event.TAG, tag);
		event.addValue(Event.TIMESTAMP, t);
		event.addValue(Event.TIMESTAMP, t.plus(d));
		event.setValue(Event.DURATION, d);
		if (d.isLongerThan(Duration.ZERO)) {
			Duration sleep = sleepDurationFromDetails(node.path("sleepDetails"));
			int efficiency = Math.min(100, Ints.checkedCast(100 * sleep.getStandardSeconds() / d.getStandardSeconds()));
			event.setValue(Event.PERCENTAGE, Percentage.valueOf(efficiency));
		}
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private Duration sleepDurationFromDetails(JsonNode node) {
		Duration sleep = Duration.ZERO;
		DateTime t0 = null;
		for (JsonNode detailNode : node) {
			DateTime t1 = dateTimeValue(detailNode.path("datetime"));
			if (t0 != null && detailNode.path("value").intValue() > 1) {
				sleep = sleep.plus(new Duration(t0, t1));
			}
			t0 = t1;
		}
		return sleep;
	}
}
