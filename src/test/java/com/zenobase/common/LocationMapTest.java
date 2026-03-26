package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Range;
import org.joda.time.DateTime;
import org.junit.Test;

import com.zenobase.models.Location;

public class LocationMapTest {

	@Test
	public void test() {

		DateTime t0 = DateTime.parse("2014-03-01T12:00:00.000Z");
		DateTime t1 = DateTime.parse("2014-03-01T13:00:00.000Z");
		DateTime t2 = DateTime.parse("2014-03-01T14:00:00.000Z");
		Location l0 = new Location("47.6204", "-122.3491");
		Location l1 = new Location("47.7315", "-122.4502");

		LocationMap locations = new LocationMap();
		assertThat(locations.contains(t0)).isFalse();
		assertThat(locations.get(t0)).isNull();

		locations.put(t0, t1, l0);
		assertThat(locations.contains(t0)).isTrue();
		assertThat(locations.contains(t1)).isFalse();
		assertThat(locations.get(t0)).isEqualTo(l0);
		assertThat(locations.get(t0.plusMinutes(5))).isEqualTo(l0);

		locations.put(t1, t2, l1);
		assertThat(locations.contains(t0)).isTrue();
		assertThat(locations.contains(t1)).isTrue();
		assertThat(locations.get(t0)).isEqualTo(l0);
		assertThat(locations.get(t1)).isEqualTo(l1);
		assertThat(locations.get(t1.plusMinutes(5))).isEqualTo(l1);

		locations.remove(t1);
		assertThat(locations.contains(t0)).isFalse();
		assertThat(locations.contains(t1)).isTrue();
	}

	@Test
	public void testTimeRange() {

		DateTime t0 = DateTime.parse("2014-03-01T12:00:00.000Z");
		DateTime t1 = DateTime.parse("2014-03-01T13:00:00.000Z");
		DateTime t2 = DateTime.parse("2014-03-01T14:00:00.000Z");
		DateTime t3 = DateTime.parse("2014-03-01T15:00:00.000Z");
		DateTime t4 = DateTime.parse("2014-03-01T16:00:00.000Z");
		Location l0 = new Location("47.6204", "-122.3491");
		Location l1 = new Location("47.7315", "-122.4502");

		LocationMap locations = new LocationMap();
		locations.put(t1, t2, l0);
		locations.put(t2, t3, l1);

		assertThat(locations.contains(Range.closedOpen(t0, t1.minusMinutes(5)))).isFalse();
		assertThat(locations.contains(Range.closedOpen(t0, t1))).isTrue();
		assertThat(locations.contains(Range.closedOpen(t1, t2))).isTrue();
		assertThat(locations.contains(Range.closedOpen(t2, t3))).isTrue();
		assertThat(locations.contains(Range.closedOpen(t3, t4))).isTrue();
		assertThat(locations.contains(Range.closedOpen(t1, t3))).isTrue();
		assertThat(locations.contains(Range.closedOpen(t0, t4))).isTrue();
		assertThat(locations.contains(Range.closedOpen(t3.plusMinutes(5), t4))).isFalse();

		assertThat(locations.getFirst(Range.closedOpen(t1, t2))).isEqualTo(l0);
		assertThat(locations.getFirst(Range.closedOpen(t2, t3))).isEqualTo(l1);
		assertThat(locations.getFirst(Range.closedOpen(t0, t4))).isEqualTo(l0);
		assertThat(locations.getFirst(Range.closedOpen(t1.plusMinutes(5), t2.minusMinutes(5))))
				.isEqualTo(l0);
		assertThat(locations.getFirst(Range.closedOpen(t0, t1))).isNull();
		assertThat(locations.getFirst(Range.closedOpen(t3, t4))).isNull();
	}
}
