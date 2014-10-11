package com.zenobase.tasks.netatmo;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class MeasurementsResultTest extends ResultTestSupport {

	@Test
	public void test() {
		DateTimeZone tz = DateTimeZone.forOffsetHours(-7);
		Device device = new Device("1", "test", DateTime.now(tz), DateTime.now(tz), new Location("1", "2"));
		MeasurementsResult result = new MeasurementsResult(TESTER, device, readObject("MeasurementsResultTest.json"));
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(10);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, DateTime.parse("2013-03-28T16:38:45.000-07:00").withZone(DateTimeZone.forOffsetHours(-7)));
		expected.addValue(Event.TAG, device.getLabel());
		expected.setValue(Event.LOCATION, device.getLocation());
		expected.setValue(Event.TEMPERATURE, Measures.<Temperature>valueOf("25.7 C"));
		expected.setValue(Event.PRESSURE, Measures.<Pressure>valueOf("1009.8 hPa"));
		expected.setValue(Event.SOUND, Measures.<Dimensionless>valueOf("40 dB"));
		expected.setValue(Event.HUMIDITY, 48);
		expected.setValue(Event.RATING, Rating.valueOf(80));
		expected.setValue(Event.HEIGHT, Measures.<Length>valueOf("11 mm"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, MeasurementsResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
		assertThat(events.get(1).getValue(Event.SOUND)).as("second event has no sound level").isNull();
	}
}
