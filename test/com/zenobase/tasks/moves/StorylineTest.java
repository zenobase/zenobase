package com.zenobase.tasks.moves;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.junit.Test;

import com.zenobase.models.Location;

public class StorylineTest {

	@Test
	public void test() {

		DateTime t0 = DateTime.parse("2014-03-01T12:00:00.000Z");
		DateTime t1 = DateTime.parse("2014-03-01T13:00:00.000Z");
		DateTime t2 = DateTime.parse("2014-03-01T14:00:00.000Z");
		Location l0 = new Location("47.6204", "-122.3491");
		Location l1 = new Location("47.7315", "-122.4502");

		Storyline storyline = new Storyline();
		assertThat(storyline.contains(t0)).isFalse();
		assertThat(storyline.get(t0)).isNull();

		storyline.put(t0, t1, l0);
		assertThat(storyline.contains(t0)).isTrue();
		assertThat(storyline.contains(t1)).isFalse();
		assertThat(storyline.get(t0)).isEqualTo(l0);
		assertThat(storyline.get(t0.plusMinutes(5))).isEqualTo(l0);

		storyline.put(t1, t2, l1);
		assertThat(storyline.contains(t0)).isTrue();
		assertThat(storyline.contains(t1)).isTrue();
		assertThat(storyline.get(t0)).isEqualTo(l0);
		assertThat(storyline.get(t1)).isEqualTo(l1);
		assertThat(storyline.get(t1.plusMinutes(5))).isEqualTo(l1);

		storyline.remove(t1);
		assertThat(storyline.contains(t0)).isFalse();
		assertThat(storyline.contains(t1)).isTrue();
	}
}
