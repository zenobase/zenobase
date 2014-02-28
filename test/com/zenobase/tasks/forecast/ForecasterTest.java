package com.zenobase.tasks.forecast;

import static org.fest.assertions.Assertions.assertThat;

import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Assume;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Location;

public class ForecasterTest {

	private final String apiKey = System.getProperty("forecast.apiKey");

	public void setUp() {
		Assume.assumeNotNull(apiKey);
	}

	@Test
	public void test() {
		Forecaster forecaster = new Forecaster(apiKey);
		Forecast expected = new Forecast("Drizzle", Measures.<Temperature>valueOf("4.26 C"), Measures.<Pressure>valueOf("1020.51 hPa"), Integer.valueOf(90));
		Forecast forecast = forecaster.find(new Location("47.6097", "-122.3331"), DateTime.parse("2014-02-25T03:15:05.100-08:00", ISODateTimeFormat.dateTime().withOffsetParsed()), true);
		assertThat(forecast).isEqualTo(expected);
	}
}
