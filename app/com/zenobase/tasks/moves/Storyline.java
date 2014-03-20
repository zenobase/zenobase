package com.zenobase.tasks.moves;

import org.joda.time.DateTime;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import com.zenobase.models.Event;
import com.zenobase.models.Location;

public class Storyline {

	private final RangeMap<DateTime, Location> locations = TreeRangeMap.create();

	public void put(DateTime begin, DateTime end, Location location) {
		locations.put(Range.closedOpen(begin, end), location);
	}

	public Location get(DateTime time) {
		return locations.get(time);
	}

	public boolean contains(DateTime time) {
		return !locations.asMapOfRanges().isEmpty()
			&& locations.span().contains(time);
	}

	public void remove(DateTime olderThan) {
		locations.remove(Range.lessThan(olderThan));
	}

	public Event update(Event event) {
		DateTime time = event.getValue(Event.TIMESTAMP);
		Location location = get(time);
		if (location == null) {
			return null;
		}
		Event copy = event.copy();
		copy.setValue(Event.LOCATION, location);
		copy.addValue(Event.SOURCE, ActivitiesResult.SOURCE);
		return copy;
	}

	@Override
	public String toString() {
		return locations.toString();
	}
}
