package com.zenobase.tasks.lastfm;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class TrackInfoResultTest extends ResultTestSupport {

	@Test
	public void test() {
		TrackInfo track = new TrackInfoResult(readObject("TrackInfoResultTest.json")).get();
		assertThat(track.getResource()).isEqualTo(new Resource("The Chemical Brothers - Galvanize", "https://www.last.fm/music/The+Chemical+Brothers/_/Galvanize"));
		assertThat(track.getDuration()).isEqualTo(Duration.standardSeconds(163));
		assertThat(track.getTags()).containsExactly("electronic", "dance", "electronica", "big beat", "techno");
	}
}
