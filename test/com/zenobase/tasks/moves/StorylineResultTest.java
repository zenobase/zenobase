package com.zenobase.tasks.moves;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import com.zenobase.models.Location;
import com.zenobase.tasks.ResultTestSupport;

public class StorylineResultTest extends ResultTestSupport {

	private final Storyline storyline = new Storyline();

	@Test
	public void test() {
		new StorylineResult(readArray("StorylineResultTest.json")).update(storyline);
		checkTime("2014-02-01T00:00:00-08:00", null);
		checkTime("2014-03-17T12:00:00-07:00", new Location("47.6269647899", "-122.3615975031"));
		checkTime("2014-03-17T12:51:47-07:00", new Location("47.6269112415", "-122.36156368"));
		checkTime("2014-03-17T12:52:05-07:00", new Location("47.6269112415", "-122.36156368"));
		checkTime("2014-03-17T13:32:00-07:00", null);
	}

	private void checkTime(String time, Location location) {
		assertThat(storyline.get(dateTime(time))).as("at " + time + " in " + storyline).isEqualTo(location);
	}
}
