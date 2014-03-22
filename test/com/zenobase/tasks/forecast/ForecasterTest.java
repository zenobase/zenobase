package com.zenobase.tasks.forecast;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Set;

import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.ImmutableSet;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Percentage;

public class ForecasterTest {

	private final String apiKey = System.getProperty("forecast.apiKey");

	@Before
	public void setUp() {
		Assume.assumeNotNull(apiKey);
	}

	@Test
	public void test() {
		Forecaster forecaster = new Forecaster(apiKey);
		Forecast expected = new Forecast("Light Rain", Measures.<Temperature>valueOf("6.78 C"), Measures.<Pressure>valueOf("1024.18 hPa"), 94, Percentage.valueOf(74));
		Set<String> fields = ImmutableSet.of(Event.TAG.getName(), Event.TEMPERATURE.getName(), Event.PRESSURE.getName(), Event.HUMIDITY.getName(), Event.MOON.getName());
		Forecast forecast = forecaster.find(new Location("47.6097", "-122.3331"), DateTime.parse("2013-02-28T03:15:05.100-08:00", ISODateTimeFormat.dateTime().withOffsetParsed()), fields, true);
		assertThat(forecast).isEqualTo(expected);
	}
}
