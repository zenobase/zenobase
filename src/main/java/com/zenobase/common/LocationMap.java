package com.zenobase.common;

import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import org.joda.time.DateTime;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Event;
import com.zenobase.models.Location;

public class LocationMap {

	private final RangeMap<DateTime, Location> locations = TreeRangeMap.create();

	public void put(DateTime begin, DateTime end, Location location) {
		locations.put(Range.closedOpen(begin, end), location);
	}

	public @Nullable Location get(DateTime time) {
		return locations.get(time);
	}

	public @Nullable Location getFirst(Range<DateTime> time) {
		return Iterables.getFirst(locations.subRangeMap(time).asMapOfRanges().values(), null);
	}

	public boolean contains(DateTime time) {
		return !locations.asMapOfRanges().isEmpty() && locations.span().contains(time);
	}

	public boolean contains(Range<DateTime> time) {
		return !locations.asMapOfRanges().isEmpty() && locations.span().isConnected(time);
	}

	public void remove(DateTime olderThan) {
		locations.remove(Range.lessThan(olderThan));
	}

	public @Nullable Event update(Event event) {
		ImmutableList<DateTime> times = event.getValues(Event.TIMESTAMP);
		Preconditions.checkState(!times.isEmpty());
		Location location =
			times.size() == 1
				? get(times.getFirst())
				: getFirst(
						Range.closedOpen(
							Objects.requireNonNull(Ordering.natural().min(times)),
							Objects.requireNonNull(Ordering.natural().max(times))
						)
					);
		if (location == null) {
			return null;
		}
		Event copy = event.copy();
		copy.setValue(Event.LOCATION, location);
		return copy;
	}

	@Override
	public String toString() {
		return locations.toString();
	}
}
