package com.zenobase.tasks.jawbone;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;

import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.tasks.ResultTestSupport;

public class FoodResultTest extends ResultTestSupport {

	@Test
	public void test() {

		FoodResult result = new FoodResult(readObject("FoodResultTest.json"), TESTER, "Meal");
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(2);

		Event meal1 = new Event(events.get(0).getId());
		meal1.setValue(Event.TIMESTAMP, dateTime("2014-10-16T09:35:37-07:00"));
		meal1.addValue(Event.TAG, "Meal");
		meal1.setValue(Event.ENERGY, Measures.valueOf("224 kcal"));
		meal1.setValue(Event.AUTHOR, TESTER);
		meal1.setValue(Event.SOURCE, JawboneResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(meal1);

		Event meal2 = new Event(events.get(1).getId());
		meal2.setValue(Event.TIMESTAMP, dateTime("2014-10-16T12:48:27-07:00"));
		meal2.addValue(Event.TAG, "Meal");
		meal2.setValue(Event.ENERGY, Measures.valueOf("460 kcal"));
		meal2.setValue(Event.LOCATION, new Location("47.62372494", "-122.3568806"));
		meal2.setValue(Event.AUTHOR, TESTER);
		meal2.setValue(Event.SOURCE, JawboneResult.SOURCE);
		assertThat(events.get(1)).as("second event").isEqualTo(meal2);
	}
}
