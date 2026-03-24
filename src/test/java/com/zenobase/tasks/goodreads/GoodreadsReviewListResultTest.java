package com.zenobase.tasks.goodreads;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class GoodreadsReviewListResultTest extends ResultTestSupport {

	@Test
	public void test() {

		GoodreadsReviewListResult result = new GoodreadsReviewListResult(readXml("review_list.xml"), TESTER, "Book");
		List<Event> actual = result.getEvents(dateTime("2018-04-14T12:00:00Z"));
		assertThat(actual.size()).isEqualTo(1);

		Event expected = new Event(actual.get(0).getId());
		expected.addValue(Event.TIMESTAMP, dateTime("2018-04-01T20:57:22-07:00"));
		expected.addValue(Event.TIMESTAMP, dateTime("2018-04-14T20:57:22-07:00"));
		expected.setValue(Event.DURATION, Duration.standardDays(13));
		expected.addValue(Event.TAG, "Book");
		expected.setValue(Event.RATING, Rating.valueOf(80));
		expected.setValue(Event.COUNT, 100);
		expected.setValue(Event.RESOURCE, new Resource(
			"The First 20 Minutes: Surprising Science Reveals How We Can: Exercise Better, Train Smarter, Live Longer",
			"https://www.goodreads.com/review/show/2347385187"
		));
		expected.setValue(Event.SOURCE, GoodreadsReviewListResult.SOURCE);
		expected.setValue(Event.AUTHOR, TESTER);
		assertThat(actual.get(0)).isEqualTo(expected);

		assertThat(result.getStartPage()).as("start page").isEqualTo(1);
		assertThat(result.getEndPage()).as("end page").isEqualTo(5);
	}
}
