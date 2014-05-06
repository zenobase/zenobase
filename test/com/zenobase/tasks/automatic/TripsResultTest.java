package com.zenobase.tasks.automatic;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.joda.time.DateTime;
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
		TripsResult result = new TripsResult(readArray("TripsResultTest.json"), TESTER, false);
		Trip trip = Iterables.getOnlyElement(result.getTrips());
		assertThat(trip.getVehicleId()).isEqualTo("xxx");
		assertThat(trip.getEvent().getValue(Event.TIMESTAMP)).isEqualTo(DateTime.parse("2013-11-02T20:14:10.201-07:00"));
		assertThat(trip.getEvent().getValue(Event.DURATION)).isEqualTo(Duration.standardSeconds(1500));
		assertThat(trip.getEvent().getValues(Event.LOCATION)).containsExactly(new Location("37.7692903", "-122.4465469"), new Location("37.78270046281092", "-122.4064556183999"));
		assertThat(trip.getEvent().getValue(Event.CURRENCY)).isEqualTo(new BigDecimal("1.0428111627932486"));
		assertThat(trip.getEvent().getValue(Event.SOURCE)).isEqualTo(TripsResult.SOURCE);
		assertThat(trip.getEvent().getValue(Event.AUTHOR)).isEqualTo(TESTER);
		assertThat(trip.getEvent().getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf(new BigDecimal("4.084532"), NonSI.MILE));
		assertThat(trip.getEvent().getValue(Event.VOLUME)).isEqualTo(Measures.valueOf(new BigDecimal("0.2465857561582522"), NonSI.GALLON_LIQUID_US));
	}

	@Test
	public void testWithMetricUnits() {
		TripsResult result = new TripsResult(readArray("TripsResultTest.json"), TESTER, true);
		Trip trip = Iterables.getOnlyElement(result.getTrips());
		assertThat(trip.getEvent().getValue(Event.DISTANCE)).isEqualTo(Measures.valueOf(new BigDecimal("6.573417"), SI.KILOMETER));
		assertThat(trip.getEvent().getValue(Event.VOLUME)).isEqualTo(Measures.valueOf(new BigDecimal("0.9334288"), NonSI.LITER));
	}
}
