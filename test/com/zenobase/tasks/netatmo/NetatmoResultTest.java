package com.zenobase.tasks.netatmo;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import org.joda.time.DateTime;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.tasks.ResultTestSupport;

public class NetatmoResultTest extends ResultTestSupport {

	@Test
	public void test() {
		Device device = new Device("1", "test", DateTime.now(), new Location("1", "2"));
		NetatmoResult result = new NetatmoResult(TESTER, device, readObject("NetatmoResultTest.json"));
		assertThat(result.getStatus()).as("status").isEqualTo("ok");
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(1024);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, DateTime.parse("2013-03-28T16:38:45.000-07:00"));
		expected.addValue(Event.TAG, device.getLabel());
		expected.setValue(Event.LOCATION, device.getLocation());
		expected.setValue(Event.TEMPERATURE, Measures.<Temperature>valueOf("25.7 C"));
		expected.setValue(Event.PRESSURE, Measures.<Pressure>valueOf("1009.8 mbar"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, NetatmoResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
