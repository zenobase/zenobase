package com.zenobase.tasks.dash;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class TripsResultTest extends ResultTestSupport {

	@Test
	public void testMilesPerUSGallon() {
		List<Event> events = readTrips("UserResultTest-MilesPerUSGallon.json", "TripsResultTest.json");
		assertThat(events).hasSize(3);
		Event event = events.get(0);
		assertThat(event.getValues(Event.TAG)).containsExactly("trip", "pro");
		assertThat(event.getValues(Event.TIMESTAMP)).containsExactly(dateTime("2014-08-22T12:35:00-07:00"), dateTime("2014-08-22T12:45:00-07:00"));
		assertThat(event.getValue(Event.DURATION)).isEqualTo(Duration.standardMinutes(10));
		assertThat(event.getValues(Event.LOCATION)).containsExactly(new Location("40.7270733", "-74.0061488"), new Location("40.7270986", "-74.0061726"));
		assertThat(event.getValue(Event.RATING)).isEqualTo(Rating.valueOf(70));
		assertThat(event.getValue(Event.SOURCE)).isEqualTo(TripsResult.SOURCE);
		assertThat(event.getValue(Event.AUTHOR)).isEqualTo(TESTER);
		assertThat(event.getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("6.28 mi"));
		assertThat(event.getValue(Event.VOLUME)).isEqualTo(Measures.valueOf("0.24 gal"));
		assertThat(event.getValue(Event.DISTANCE_PER_VOLUME)).isEqualTo(Measures.valueOf("26.00 mpg"));
	}

	@Test
	public void testMilesPerImperialGallon() {
		List<Event> events = readTrips("UserResultTest-MilesPerImperialGallon.json", "TripsResultTest.json");
		assertThat(events).hasSize(3);
		Event event = events.get(0);
		assertThat(event.getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("6.28 mi"));
		assertThat(event.getValue(Event.VOLUME)).isEqualTo(Measures.valueOf("0.29 gal"));
		assertThat(event.getValue(Event.DISTANCE_PER_VOLUME)).isEqualTo(Measures.valueOf("21.65 mpg"));
	}

	@Test
	public void testKilometersPerLiter() {
		List<Event> events = readTrips("UserResultTest-KilometersPerLiter.json", "TripsResultTest.json");
		assertThat(events).hasSize(3);
		Event event = events.get(0);
		assertThat(event.getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("6.28 km"));
		assertThat(event.getValue(Event.VOLUME)).isEqualTo(Measures.valueOf("0.24 L"));
		assertThat(event.getValue(Event.DISTANCE_PER_VOLUME)).isEqualTo(Measures.valueOf("26.00 kpl"));
	}

	@Test
	public void testLiterPer100Kilometer() {
		List<Event> events = readTrips("UserResultTest-LiterPer100Kilometer.json", "TripsResultTest.json");
		assertThat(events).hasSize(3);
		Event event = events.get(0);
		assertThat(event.getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("6.28 km"));
		assertThat(event.getValue(Event.VOLUME)).isEqualTo(Measures.valueOf("0.24 L"));
		assertThat(event.getValue(Event.DISTANCE_PER_VOLUME)).isEqualTo(Measures.valueOf("3.85 kpl"));
	}

	private final List<Event> readTrips(String settingsFile, String tripsFile) {
		UserSettings settings = new UserSettingsResult(readObject(settingsFile)).get();
		DateTimeZone timezone = DateTimeZone.forID("America/Los_Angeles");
		return new TripsResult(readObject(tripsFile), TESTER, settings, "trip", timezone).getTrips();
	}
}
