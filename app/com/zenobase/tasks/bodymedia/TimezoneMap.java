package com.zenobase.tasks.bodymedia;

import java.util.Set;

import org.elasticsearch.common.collect.Sets;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

public class TimezoneMap {

	private final RangeMap<DateTime, DateTimeZone> map = TreeRangeMap.create();
	private final Set<DateTimeZone> set = Sets.newLinkedHashSet();

	public void add(DateTime from, DateTime to, DateTimeZone timezone) {
		if (map.asMapOfRanges().isEmpty()) {
			from = from.withTimeAtStartOfDay();
		}
		map.put(to != null ? Range.closedOpen(from, to) : Range.atLeast(from), timezone);
		set.add(timezone);
	}

	public LocalDate getBegin() {
		return map.span().lowerEndpoint().toLocalDate();
	}

	public DateTime getBegin(LocalDate date) {
		DateTime time = null;
		for (LocalDateTime local = date.toLocalDateTime(LocalTime.MIDNIGHT); time == null; local = local.plusMinutes(1)) {
			time = zone(local);
		}
		return time;
	}

	private DateTime zone(LocalDateTime local) {
		DateTime time = null;
		for (DateTimeZone timezone : set) {
			if (!timezone.isLocalDateTimeGap(local)) {
				if (timezone.equals(map.get(local.toDateTime(timezone)))) {
					DateTime t = local.toDateTime(timezone);
					if (time == null || t.isBefore(time)) {
						time = t;
					}
				}
			}
		}
		return time;
	}

	public DateTime rezone(DateTime time) {
		DateTimeZone zone = map.get(time);
		if (zone == null) {
			return null;
		}
		if (zone.equals(time.getZone())) {
			return time;
		}
		return time.withZone(zone);
	}
}
