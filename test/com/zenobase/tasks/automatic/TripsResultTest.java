package com.zenobase.tasks.automatic;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;

import org.joda.time.Duration;
import org.junit.Test;
import com.google.common.collect.Iterables;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class TripsResultTest extends ResultTestSupport {

	@Test
	public void testWithImperialUnits() {
		TripsResult result = new TripsResult(readObject("TripsResultTest.json"), TESTER, "trip", false);
		assertThat(result.hasNext()).isTrue();
		Trip trip = Iterables.getOnlyElement(result.getTrips());
		assertThat(trip.getVehicleId()).isEqualTo("https://api.automatic.com/vehicle/C_c10bf058767b0f67/");
		assertThat(trip.getEvent().getValues(Event.TAG)).containsExactly("trip", "test");
		assertThat(trip.getEvent().getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2015-05-26T13:50:55-07:00"));
		assertThat(trip.getEvent().getValue(Event.DURATION)).isEqualTo(Duration.standardSeconds(157));
		assertThat(trip.getEvent().getValues(Event.LOCATION)).containsExactly(new Location("47.62524", "-122.35841"), new Location("47.62703", "-122.36167"));
		assertThat(trip.getEvent().getValue(Event.CURRENCY)).isEqualTo(new BigDecimal("0.06"));
		assertThat(trip.getEvent().getValue(Event.RATING)).isEqualTo(Rating.valueOf(90));
		assertThat(trip.getEvent().getValue(Event.SOURCE)).isEqualTo(TripsResult.SOURCE);
		assertThat(trip.getEvent().getValue(Event.AUTHOR)).isEqualTo(TESTER);
		assertThat(trip.getEvent().getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("0.22 mi"));
		assertThat(trip.getEvent().getValue(Event.VOLUME)).isEqualTo(Measures.valueOf("0.03 gal"));
		assertThat(trip.getEvent().getValue(Event.DISTANCE_PER_VOLUME)).isEqualTo(Measures.valueOf("10.58 mpg"));
	}

	@Test
	public void testWithMetricUnits() {
		TripsResult result = new TripsResult(readObject("TripsResultTest.json"), TESTER, "Trip", true);
		Trip trip = Iterables.getOnlyElement(result.getTrips());
		assertThat(trip.getEvent().getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("0.35 km"));
		assertThat(trip.getEvent().getValue(Event.VOLUME)).isEqualTo(Measures.valueOf("0.10 L"));
		assertThat(trip.getEvent().getValue(Event.DISTANCE_PER_VOLUME)).isEqualTo(Measures.valueOf("4.50 kpl"));
	}
}
