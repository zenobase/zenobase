package com.zenobase.tasks.rescuetime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.jupiter.api.Test;

import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class CategoriesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		DateTimeZone timezone = DateTimeZone.forOffsetHours(-7);
		ProductivityResult result =
				new ProductivityResult(readObject("CategoriesResultTest.json"), TESTER, null, timezone);
		assertThat(result.isSuccess()).isTrue();
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(3);
		Event expected = new Event(events.get(0).getId());
		expected.addValue(Event.TAG, "Social Networking");
		expected.setValue(Event.TIMESTAMP, dateTime("2014-05-01T05:00:00-07:00"));
		expected.setValue(Event.DURATION, Duration.standardSeconds(192));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, ProductivityResult.SOURCE);
		assertThat(events.get(0)).isEqualTo(expected);
	}
}
