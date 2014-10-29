package com.zenobase.tasks.automatic;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;

import org.joda.time.Duration;
import org.junit.Test;
import com.google.common.collect.Iterables;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.tasks.ResultTestSupport;

public class TripsResultTest extends ResultTestSupport {

	@Test
	public void testWithImperialUnits() {
		TripsResult result = new TripsResult(readArray("TripsResultTest.json"), TESTER, "Trip", false);
		Trip trip = Iterables.getOnlyElement(result.getTrips());
		assertThat(trip.getVehicleId()).isEqualTo("xxx");
		assertThat(trip.getEvent().getValue(Event.TAG)).isEqualTo("Trip");
		assertThat(trip.getEvent().getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2013-11-02T20:14:10-07:00"));
		assertThat(trip.getEvent().getValue(Event.DURATION)).isEqualTo(Duration.standardSeconds(1500));
		assertThat(trip.getEvent().getValues(Event.LOCATION)).containsExactly(new Location("37.7692903", "-122.4465469"), new Location("37.78270046281092", "-122.4064556183999"));
		assertThat(trip.getEvent().getValue(Event.CURRENCY)).isEqualTo(new BigDecimal("1.04"));
		assertThat(trip.getEvent().getValue(Event.SOURCE)).isEqualTo(TripsResult.SOURCE);
		assertThat(trip.getEvent().getValue(Event.AUTHOR)).isEqualTo(TESTER);
		assertThat(trip.getEvent().getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("4.08 mi"));
		assertThat(trip.getEvent().getValue(Event.VOLUME)).isEqualTo(Measures.valueOf("0.25 gal"));
		assertThat(trip.getEvent().getValue(Event.DISTANCE_PER_VOLUME)).isEqualTo(Measures.valueOf("16.56 mpg"));
	}

	@Test
	public void testWithMetricUnits() {
		TripsResult result = new TripsResult(readArray("TripsResultTest.json"), TESTER, "Trip", true);
		Trip trip = Iterables.getOnlyElement(result.getTrips());
		assertThat(trip.getEvent().getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf("6.57 km"));
		assertThat(trip.getEvent().getValue(Event.VOLUME)).isEqualTo(Measures.valueOf("0.93 L"));
		assertThat(trip.getEvent().getValue(Event.DISTANCE_PER_VOLUME)).isEqualTo(Measures.valueOf("7.04 kpl"));
	}

	@Test
	public void testMinimalTrip() {
		TripsResult result = new TripsResult(readArray("TripsResultTest-Minimal.json"), TESTER, "Trip", false);
		Trip trip = Iterables.getOnlyElement(result.getTrips());
		assertThat(trip.getVehicleId()).isNull();
		assertThat(trip.getEvent().getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2013-11-03T03:14:10Z"));
		assertThat(trip.getEvent().getValue(Event.DURATION)).isEqualTo(Duration.standardSeconds(1500));
	}
}
