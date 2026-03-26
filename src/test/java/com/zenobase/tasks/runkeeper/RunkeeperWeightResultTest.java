package com.zenobase.tasks.runkeeper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class RunkeeperWeightResultTest extends ResultTestSupport {

	private final Identity author = new Identity();

	@Test
	public void test() {

		RunkeeperWeightResult result = new RunkeeperWeightResult(
				readObject("RunkeeperWeightResultTest.json"),
				author,
				"Me",
				Units.KG,
				DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.getNext()).isEqualTo("/weight?page=1&pageSize=1");
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(2);

		Event e1 = new Event(events.get(0).getId());
		e1.addValue(Event.TAG, "Me");
		e1.setValue(Event.TIMESTAMP, dateTime("2014-11-07T18:43:23-08:00"));
		e1.setValue(Event.WEIGHT, Measures.valueOf("70.80 kg"));
		e1.setValue(Event.PERCENTAGE, Percentage.valueOf(new BigDecimal("11.8")));
		e1.setValue(Event.SOURCE, new Resource("RunKeeper", "/weight/-16108705-1415385803089"));
		e1.setValue(Event.AUTHOR, author);
		assertThat(events.get(0)).isEqualTo(e1);

		Event e2 = new Event(events.get(1).getId());
		e2.addValue(Event.TAG, "Me");
		e2.setValue(Event.TIMESTAMP, dateTime("2014-11-06T19:47:18-08:00"));
		e2.setValue(Event.WEIGHT, Measures.valueOf("70.70 kg"));
		e2.setValue(Event.SOURCE, new Resource("RunKeeper", "/weight/-16108705-1415303238657"));
		e2.setValue(Event.AUTHOR, author);
		assertThat(events.get(1)).isEqualTo(e2);
	}
}
