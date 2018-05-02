package com.zenobase.tasks.trakt;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class TraktHistoryResultTest extends ResultTestSupport {

	@Test
	public void testMovies() {

		TraktHistoryResult result = new TraktHistoryResult(readArray("TraktMoviesResultTest.json"), TESTER,
			DateTime.parse("2015-04-04T04:30:00.000Z"), DateTimeZone.forID("America/Los_Angeles"));
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);

		assertThat(events.get(0).getValues(Event.TAG)).containsExactly("movie", "animation", "family", "fantasy");
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(DateTime.parse("2015-04-04T21:00:00-07:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardMinutes(93));
		assertThat(events.get(0).getValue(Event.RESOURCE))
			.isEqualTo(new Resource("Song of the Sea (2014)", "https://trakt.tv/search/trakt/77888?id_type=movie"));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(TraktHistoryResult.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}

	@Test
	public void testEpisodes() {

		TraktHistoryResult result = new TraktHistoryResult(readArray("TraktEpisodesResultTest.json"), TESTER,
			DateTime.parse("2015-04-15T04:45:00.000Z"), DateTimeZone.forID("America/Los_Angeles"));
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(1);

		assertThat(events.get(0).getValues(Event.TAG)).containsExactly("episode", "drama");
		assertThat(events.get(0).getValue(Event.TIMESTAMP)).isEqualTo(DateTime.parse("2015-04-14T23:30:00-07:00"));
		assertThat(events.get(0).getValue(Event.DURATION)).isEqualTo(Duration.standardMinutes(60));
		assertThat(events.get(0).getValue(Event.RESOURCE))
			.isEqualTo(new Resource("Game of Thrones: The Wars to Come (Season 5, Episode 1)", "https://trakt.tv/search/trakt/73680?id_type=episode"));
		assertThat(events.get(0).getValue(Event.SOURCE)).isEqualTo(TraktHistoryResult.SOURCE);
		assertThat(events.get(0).getValue(Event.AUTHOR)).isEqualTo(TESTER);
	}
}
