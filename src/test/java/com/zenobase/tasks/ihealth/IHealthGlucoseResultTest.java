package com.zenobase.tasks.ihealth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.tasks.ResultTestSupport;

public class IHealthGlucoseResultTest extends ResultTestSupport {

	@Test
	public void test() {

		String tag = "Glucose";
		IHealthGlucoseResult result = new IHealthGlucoseResult(
				readObject("IHealthGlucoseResultTest.json"), TESTER, tag, DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.hasNext()).isFalse();
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo(tag);
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2014-12-11T12:30:49-08:00"));
		assertThat(events.get(0).getValue(Event.CONCENTRATION)).isEqualTo(Measures.valueOf("81 mg/dL"));
		assertThat(events.get(0).getValue(Event.LOCATION)).isEqualTo(new Location("47.6097", "-122.3331"));
		assertThat(events.get(0).getValue(Event.NOTE)).isEqualTo("testing");
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(IHealthResultSupport.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
