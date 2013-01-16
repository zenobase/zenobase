package com.zenobase.tasks.fitbit;

import static org.fest.assertions.Assertions.assertThat;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.ResultTestSupport;
import com.zenobase.tasks.fitbit.FitbitProfileResult;

public class FitbitProfileResultTest extends ResultTestSupport {

	@Test
	public void test() {
		FitbitProfileResult result = new FitbitProfileResult(readObject("FitbitProfileResultTest.json"));
		assertThat(result.getTimezone()).as("time zone").isEqualTo(DateTimeZone.forOffsetHours(-8));
		assertThat(result.getDistanceLocale()).as("distance locale").isEqualTo("en_US");
		assertThat(result.getDistanceUnit()).as("distance unit").isEqualTo(NonSI.MILE);
		assertThat(result.getHeightUnit()).as("height unit").isEqualTo(NonSI.FOOT);
	}

	@Test
	public void testEmpty() {
		FitbitProfileResult result = new FitbitProfileResult(Nodes.newObject());
		assertThat(result.getTimezone()).as("time zone").isEqualTo(DateTimeZone.UTC);
		assertThat(result.getDistanceLocale()).as("distance locale").isNull();
		assertThat(result.getDistanceUnit()).as("distance unit").isEqualTo(SI.KILOMETER);
		assertThat(result.getHeightUnit()).as("height unit").isEqualTo(SI.METER);
	}
}
