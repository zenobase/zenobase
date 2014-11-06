package com.zenobase.tasks.runkeeper;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class RunkeeperActivityResultTest extends ResultTestSupport {

	@Test
	public void test() {
		Event event = new Event();
		new RunkeeperActivityResult(readObject("RunkeeperActivityResultTest.json"), Units.M).addDetails(event);
		assertThat(event.getValue(Event.NOTE)).isEqualTo("Nice evening walk.");
		assertThat(event.getValue(Event.HEIGHT)).isEqualTo(Measures.valueOf("58 m"));
		assertThat(event.getValue(Event.FREQUENCY)).isEqualTo(Measures.valueOf("75 bpm"));
		assertThat(event.getValue(Event.SOURCE)).isEqualTo(new Resource("RunKeeper", "http://runkeeper.com/user/ejain/activity/279540153"));
		assertThat(event.getValue(Event.LOCATION)).isEqualTo(new Location("47.626863", "-122.360985"));
	}
}
