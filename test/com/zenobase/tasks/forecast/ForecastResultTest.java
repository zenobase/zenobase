package com.zenobase.tasks.forecast;

import static org.fest.assertions.Assertions.assertThat;

import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.tasks.ResultTestSupport;

public class ForecastResultTest extends ResultTestSupport {

	@Test
	public void test() {
		ForecastResult result = new ForecastResult(readObject("ForecastResultTest.json"), true);
		Forecast expected = new Forecast("Drizzle", Measures.<Temperature>valueOf("4.27 C"), Measures.<Pressure>valueOf("1020.51 hPa"), Integer.valueOf(90));
		Forecast forecast = result.get(DateTime.parse("2014-02-25T03:15:05.100-08:00", ISODateTimeFormat.dateTime().withOffsetParsed()));
		assertThat(forecast).isEqualTo(expected);
	}
}
