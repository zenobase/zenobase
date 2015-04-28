package com.zenobase.tasks.jawbone;

import org.joda.time.DateTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Rating;

class MoodResult extends JawboneResult {

	public MoodResult(JsonNode node, Identity author, String tag) {
		super(node, author, tag);
	}

	public Event getEvent() {
		return node.path("sub_type").isInt() ? newEvent(node) : null;
	}

	private Event newEvent(JsonNode node) {
		DateTime begin = dateTimeValue(node.path("time_created"), dateTimeZoneValue(node.path("details").path("tz")));
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		String title = Strings.emptyToNull(node.path("title").textValue());
		if (title != null) {
			event.addValue(Event.TAG, title);
		}
		event.setValue(Event.TIMESTAMP, begin);
		event.setValue(Event.RATING, ratingValue(node.path("sub_type")));
		event.setValue(Event.LOCATION, locationValue(node));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	@Override
	protected Rating ratingValue(JsonNode node) {
		switch (node.intValue()) {
			case 1: return Rating.valueOf(100); // amazing, 5/5
			case 2: return Rating.valueOf(85);  // pumped up, 4/5
			case 3: return Rating.valueOf(70);  // energized, 4/5
			case 8: return Rating.valueOf(55);  // good, 3/5
			case 4: return Rating.valueOf(40);  // meh, 2/5
			case 5: return Rating.valueOf(25);  // dragging, 1/5
			case 6: return Rating.valueOf(10);  // exhausted, 1/5
			case 7: return Rating.valueOf(0);   // totally done, 0/5
		}
		throw new IllegalArgumentException("Unexpected mood value: " + node);
	}
}
