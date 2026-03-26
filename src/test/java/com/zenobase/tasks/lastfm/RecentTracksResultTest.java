package com.zenobase.tasks.lastfm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class RecentTracksResultTest extends ResultTestSupport {

	private final String tag = "Recording";
	private final DateTimeZone timezone = DateTimeZone.forOffsetHours(-8);

	@Test
	public void test() {

		RecentTracksResult result =
				new RecentTracksResult(readObject("RecentTracksResultTest.json"), TESTER, tag, timezone);
		assertThat(result.hasNext()).isTrue();
		List<Event> events = result.getEvents();
		assertThat(events).hasSize(9);

		Event e1 = new Event(events.get(0).getId());
		e1.setValue(Event.TIMESTAMP, new DateTime(1392970266000L, timezone));
		e1.setValue(
				Event.RESOURCE,
				new Resource(
						"The Chemical Brothers - Galvanize",
						"https://musicbrainz.org/recording/9dbadd08-ae0e-4d33-b3d6-43a9eb42bee0"));
		e1.setValue(Event.AUTHOR, TESTER);
		e1.setValue(Event.SOURCE, RecentTracksResult.SOURCE);
		e1.addValue(Event.TAG, tag);
		assertThat(events.get(0)).isEqualTo(e1);

		Event e2 = new Event(events.get(1).getId());
		e2.setValue(Event.TIMESTAMP, new DateTime(1392969931000L, timezone));
		e2.setValue(
				Event.RESOURCE,
				new Resource(
						"Loz Contreras - Only Me - Original Mix",
						"https://www.last.fm/music/Loz+Contreras/_/Only+Me+-+Original+Mix"));
		e2.setValue(Event.AUTHOR, TESTER);
		e2.setValue(Event.SOURCE, RecentTracksResult.SOURCE);
		e2.addValue(Event.TAG, tag);
		assertThat(events.get(1)).isEqualTo(e2);
	}
}
