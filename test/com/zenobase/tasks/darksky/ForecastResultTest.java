package com.zenobase.tasks.darksky;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

public class ForecastResultTest extends ResultTestSupport {

	@Test
	public void test() {
		ForecastResult result = new ForecastResult(readObject("ForecastResultTest.json"), true);
		Forecast expected = new Forecast("Light Rain", Measures.valueOf("6.78 C"), Measures.valueOf("1024.18 hPa"), 94, Percentage.valueOf(74));
		Forecast forecast = result.get();
		assertThat(forecast).isEqualTo(expected);
	}

	@Test
	public void testMoonPhaseToPercetage() {
		assertThat(ForecastResult.moonPhaseToPercentage(0.0)).isEqualTo(Percentage.valueOf(0));
		assertThat(ForecastResult.moonPhaseToPercentage(0.125)).isEqualTo(Percentage.valueOf(25));
		assertThat(ForecastResult.moonPhaseToPercentage(0.25)).isEqualTo(Percentage.valueOf(50));
		assertThat(ForecastResult.moonPhaseToPercentage(0.375)).isEqualTo(Percentage.valueOf(75));
		assertThat(ForecastResult.moonPhaseToPercentage(0.5)).isEqualTo(Percentage.valueOf(100));
		assertThat(ForecastResult.moonPhaseToPercentage(0.625)).isEqualTo(Percentage.valueOf(75));
		assertThat(ForecastResult.moonPhaseToPercentage(0.75)).isEqualTo(Percentage.valueOf(50));
		assertThat(ForecastResult.moonPhaseToPercentage(0.875)).isEqualTo(Percentage.valueOf(25));
		assertThat(ForecastResult.moonPhaseToPercentage(0.999)).isEqualTo(Percentage.valueOf(0));
	}
}
