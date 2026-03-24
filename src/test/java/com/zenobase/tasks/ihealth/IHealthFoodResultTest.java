package com.zenobase.tasks.ihealth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class IHealthFoodResultTest extends ResultTestSupport {

	@Test
	public void test() {

		String tag = "Food";
		IHealthFoodResult result = new IHealthFoodResult(readObject("IHealthFoodResultTest.json"), TESTER, tag, DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.hasNext()).isFalse();
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);

		assertThat(events.get(0).getValues(Event.TAG)).containsExactly(tag, "Breakfast");
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(dateTime("2014-12-11T12:30:49-08:00"));
		assertThat(events.get(0).getValue(Event.WEIGHT)).isEqualTo(Measures.valueOf("100 g"));
		assertThat(events.get(0).getValue(Event.ENERGY)).isEqualTo(Measures.valueOf("500 kcal"));
		assertThat(events.get(0).getValue(Event.LOCATION)).isNull();
		assertThat(events.get(0).getValue(Event.NOTE)).isNull();
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(IHealthResultSupport.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
