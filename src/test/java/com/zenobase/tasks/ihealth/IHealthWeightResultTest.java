package com.zenobase.tasks.ihealth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Percentage;
import com.zenobase.tasks.ResultTestSupport;

public class IHealthWeightResultTest extends ResultTestSupport {

	@Test
	public void test() {

		String tag = "Weight";
		IHealthWeightResult result = new IHealthWeightResult(
				readObject("IHealthWeightResultTest.json"), TESTER, tag, DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.hasNext()).isFalse();
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(2);

		assertThat(events.get(0).getValue(Event.TAG)).isEqualTo(tag);
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2014-12-10T07:38:10-08:00"));
		assertThat(events.get(0).getValue(Event.WEIGHT)).isEqualTo(Measures.valueOf("155.87 lb"));
		assertThat(events.get(0).getValue(Event.PERCENTAGE)).isEqualTo(Percentage.valueOf(11));
		assertThat(events.get(0).getValue(Event.NOTE)).isNull();
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(IHealthResultSupport.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);

		assertThat(events.get(1).getValue(Event.TAG)).isEqualTo(tag);
		assertThat(events.get(1).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2014-12-11T07:29:41-08:00"));
		assertThat(events.get(1).getValue(Event.WEIGHT)).isEqualTo(Measures.valueOf("154.77 lb"));
		assertThat(events.get(1).getValue(Event.PERCENTAGE)).isEqualTo(Percentage.valueOf(12));
		assertThat(events.get(1).getValue(Event.NOTE)).isEqualTo("testing");
		assertThat(events.get(1).getValue(Event.SOURCE)).isEqualTo(IHealthResultSupport.SOURCE);
		assertThat(events.get(1).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
