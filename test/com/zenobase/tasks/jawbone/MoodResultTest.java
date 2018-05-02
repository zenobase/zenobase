package com.zenobase.tasks.jawbone;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class MoodResultTest extends ResultTestSupport {

	@Test
	public void test() {

		MoodResult result = new MoodResult(readObject("MoodResultTest.json"), TESTER, "Mood");
		Event actual = result.getEvent();

		Event expected = new Event(actual.getId());
		expected.setValue(Event.TIMESTAMP, dateTime("2015-04-14T22:04:18-07:00"));
		expected.addValue(Event.TAG, "Mood");
		expected.addValue(Event.TAG, "Curious");
		expected.setValue(Event.LOCATION, new Location("47.6268198", "-122.3615735"));
		expected.setValue(Event.RATING, Rating.valueOf(40));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, JawboneResult.SOURCE);
		assertThat(actual).as("first event").isEqualTo(expected);
	}
}
