package com.zenobase.tasks.ihealth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class IHealthBloodPressureResultTest extends ResultTestSupport {

	@Test
	public void test() {

		String tag = "Cardio";
		IHealthBloodPressureResult result = new IHealthBloodPressureResult(readObject("IHealthBloodPressureResultTest.json"), TESTER, tag, DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.hasNext()).isFalse();
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo(tag);
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2014-12-11T08:55:56-08:00"));
		assertThat(events.get(0).getValues(Event.PRESSURE)).containsExactly(Measures.valueOf("50 mmHg"), Measures.valueOf("100 mmHg"));
		assertThat(events.get(0).getValue(Event.FREQUENCY)).isEqualTo(Measures.valueOf("60 bpm"));
		assertThat(events.get(0).getValue(Event.LOCATION)).isNull();
		assertThat(events.get(0).getValue(Event.NOTE)).isEqualTo("testing");
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(IHealthResultSupport.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
