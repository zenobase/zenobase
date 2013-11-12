package com.zenobase.tasks.bodymedia;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.ResultTestSupport;

public class BodyMediaBurnResultTest extends ResultTestSupport {

	@Test
	public void test() {

		Identity author = new Identity();
		RangeMap<LocalDateTime, DateTimeZone> timezones = buildTimezoneMap();
		BodyMediaBurnResult result = new BodyMediaBurnResult(readObject("BodyMediaBurnResultTest.json"), author, timezones);

		List<Event> events = result.getEvents();
		assertThat(result.getDate()).isEqualTo(LocalDate.parse("2013-11-03"));
		assertThat(events).hasSize(25);

		Event e1 = events.get(0);
		assertThat(e1.getValue(Event.TIMESTAMP)).isEqualTo(DateTime.parse("2013-11-03T00:00:00-0700"));
		assertThat(e1.getValue(Event.DURATION)).isEqualTo(Duration.standardHours(1));
		assertThat(e1.getValue(Event.ENERGY)).isEqualTo(Measures.<Energy>valueOf("98.082 cal"));

		Event e2 = events.get(1);
		assertThat(e2.getValue(Event.TIMESTAMP)).isEqualTo(DateTime.parse("2013-11-03T01:00:00-0700"));
		assertThat(e2.getValue(Event.DURATION)).isEqualTo(Duration.standardHours(1));
		assertThat(e2.getValue(Event.ENERGY)).isEqualTo(Measures.<Energy>valueOf("81.673 cal"));

		Event e3 = events.get(2);
		assertThat(e3.getValue(Event.TIMESTAMP)).isEqualTo(DateTime.parse("2013-11-03T01:00:00-0800"));
		assertThat(e3.getValue(Event.DURATION)).isEqualTo(Duration.standardHours(1));
		assertThat(e3.getValue(Event.ENERGY)).isEqualTo(Measures.<Energy>valueOf("78.222 cal"));

		Event e4 = events.get(3);
		assertThat(e4.getValue(Event.TIMESTAMP)).isEqualTo(DateTime.parse("2013-11-03T02:00:00-0800"));
		assertThat(e4.getValue(Event.DURATION)).isEqualTo(Duration.standardHours(1));
		assertThat(e4.getValue(Event.ENERGY)).isEqualTo(Measures.<Energy>valueOf("80.749 cal"));
	}

	private static RangeMap<LocalDateTime, DateTimeZone> buildTimezoneMap() {
		return ImmutableRangeMap.<LocalDateTime, DateTimeZone>builder()
			.put(Range.<LocalDateTime>all(), DateTimeZone.forID("America/Los_Angeles"))
			.build();
	}
}
