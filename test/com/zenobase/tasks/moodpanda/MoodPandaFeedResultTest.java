package com.zenobase.tasks.moodpanda;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class MoodPandaFeedResultTest extends ResultTestSupport {

	@Test
	public void test() {

		MoodPandaFeedResult result = new MoodPandaFeedResult(readXml("MoodPandaFeedResultTest.xml"), TESTER, -7.5, "Mood");
		List<Event> actual = result.getEvents(dateTime("2015-04-01T10:00:00Z"));
		assertThat(actual.size()).isEqualTo(6);

		Event expected = new Event(actual.get(1).getId());
		expected.setValue(Event.TIMESTAMP, dateTime("2015-04-10T11:35:54-06:30"));
		expected.addValue(Event.TAG, "Mood");
		expected.setValue(Event.RATING, Rating.valueOf(70));
		expected.setValue(Event.NOTE, "First #test.");
		expected.setValue(Event.SOURCE, MoodPandaFeedResult.SOURCE);
		expected.setValue(Event.AUTHOR, TESTER);
		assertThat(actual.get(1)).isEqualTo(expected);
	}
}
