package com.zenobase.tasks.trackthisforme;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class TrackthisformeElementsResultTest extends ResultTestSupport {

	private final static Resource SOURCE = new Resource("Trackthisforme", "https://www.trackthisfor.me/#/310");

	@Test
	public void test() {

		Category category = new Category("33243", "Donuts", null);
		List<Event> events = parse("TrackthisformeElementsResultTest.json", category, null, null, true);
		assertThat(events).hasSize(2);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo(category.getName());
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2013-06-04T16:13:00+02:00"));
		assertThat(events.get(0).getValue(Event.RATING)).isEqualTo(Rating.valueOf(80));
		assertThat(events.get(0).getValue(Event.NOTE)).isEqualTo("a test");
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);

		assertThat(events.get(1).getValue(Event.TAG)).isEqualTo(category.getName());
		assertThat(events.get(1).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2013-06-05T12:13:00+02:00"));
		assertThat(events.get(1).getValue(Event.RATING)).isEqualTo(Rating.valueOf(100));
		assertThat(events.get(1).getValue(Event.NOTE)).isNull();
		assertThat(events.get(1).getValue(Event.SOURCE)).isEqualTo(SOURCE);
		assertThat(events.get(1).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}

	@Test
	public void testCount() {

		Category category = new Category("33243", "Pushups", null);
		List<Event> events = parse("TrackthisformeElementsResultTest.json", category, "count", null, false);
		assertThat(events).hasSize(2);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo(category.getName());
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2013-06-04T16:13:00+02:00"));
		assertThat(events.get(0).getValue(Event.COUNT)).isEqualTo(100);
		assertThat(events.get(0).getValue(Event.RATING)).isNull();
		assertThat(events.get(0).getValue(Event.NOTE)).isEqualTo("a test");
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);

		assertThat(events.get(1).getValue(Event.TAG)).isEqualTo(category.getName());
		assertThat(events.get(1).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2013-06-05T12:13:00+02:00"));
		assertThat(events.get(1).getValue(Event.COUNT)).isEqualTo(101);
		assertThat(events.get(1).getValue(Event.RATING)).isNull();
		assertThat(events.get(1).getValue(Event.NOTE)).isNull();
		assertThat(events.get(1).getValue(Event.SOURCE)).isEqualTo(SOURCE);
		assertThat(events.get(1).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}

	@Test
	public void testDuration() {

		Category category = new Category("33243", "Running", "min");
		List<Event> events = parse("TrackthisformeElementsResultTest.json", category, "duration", null, false);
		assertThat(events).hasSize(2);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo(category.getName());
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2013-06-04T16:13:00+02:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardMinutes(100));
		assertThat(events.get(0).getValue(Event.RATING)).isNull();
		assertThat(events.get(0).getValue(Event.NOTE)).isEqualTo("a test");
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);

		assertThat(events.get(1).getValue(Event.TAG)).isEqualTo(category.getName());
		assertThat(events.get(1).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2013-06-05T12:13:00+02:00"));
		assertThat(events.get(1).getValue(Event.DURATION)).isEqualTo(Duration.standardSeconds(6090));
		assertThat(events.get(1).getValue(Event.RATING)).isNull();
		assertThat(events.get(1).getValue(Event.NOTE)).isNull();
		assertThat(events.get(1).getValue(Event.SOURCE)).isEqualTo(SOURCE);
		assertThat(events.get(1).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}

	@Test
	public void testDecimalMeasure() {

		Category category = new Category("33243", "Weight", "kg");
		List<Event> events = parse("TrackthisformeElementsResultTest.json", category, "weight", "kg", false);
		assertThat(events).hasSize(2);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo(category.getName());
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2013-06-04T16:13:00+02:00"));
		assertThat(events.get(0).getValue(Event.WEIGHT)).isEqualTo(Measures.valueOf("100.0 kg"));
		assertThat(events.get(0).getValue(Event.RATING)).isNull();
		assertThat(events.get(0).getValue(Event.NOTE)).isEqualTo("a test");
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);

		assertThat(events.get(1).getValue(Event.TAG)).isEqualTo(category.getName());
		assertThat(events.get(1).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2013-06-05T12:13:00+02:00"));
		assertThat(events.get(1).getValue(Event.WEIGHT)).isEqualTo(Measures.valueOf("101.5 kg"));
		assertThat(events.get(1).getValue(Event.RATING)).isNull();
		assertThat(events.get(1).getValue(Event.NOTE)).isNull();
		assertThat(events.get(1).getValue(Event.SOURCE)).isEqualTo(SOURCE);
		assertThat(events.get(1).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}

	private List<Event> parse(String file, Category category, String field, String unit, boolean includeRatings) {
		return new TrackthisformeElementsResult(readObject(file), TESTER, dateTime("2012-06-04T00:00:00+02:00"),
			category, field, unit, includeRatings).getEvents();
	}
}
