package com.zenobase.tasks.fitbit;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Units;
import com.zenobase.json.Nodes;
import com.zenobase.tasks.ResultTestSupport;

public class FitbitProfileResultTest extends ResultTestSupport {

	@Test
	public void test() {
		FitbitProfileResult result = new FitbitProfileResult(readObject("FitbitProfileResultTest.json"));
		assertThat(result.getTimezone()).as("time zone").isEqualTo(DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.getDistanceLocale()).as("distance locale").isEqualTo("en_US");
		assertThat(result.getDistanceUnit()).as("distance unit").isEqualTo(Units.MI);
		assertThat(result.getHeightUnit()).as("height unit").isEqualTo(Units.FT);
		assertThat(result.getWeightLocale()).as("weight locale").isEqualTo("en_US");
		assertThat(result.getWeightUnit()).as("weight unit").isEqualTo(Units.LB);
	}

	@Test
	public void testEmpty() {
		FitbitProfileResult result = new FitbitProfileResult(Nodes.newObject());
		assertThat(result.getTimezone()).as("time zone").isEqualTo(DateTimeZone.UTC);
		assertThat(result.getDistanceLocale()).as("distance locale").isNull();
		assertThat(result.getDistanceUnit()).as("distance unit").isEqualTo(Units.KM);
		assertThat(result.getHeightUnit()).as("height unit").isEqualTo(Units.M);
		assertThat(result.getWeightLocale()).as("weight locale").isNull();
		assertThat(result.getWeightUnit()).as("weight unit").isEqualTo(Units.KG);
	}
}
