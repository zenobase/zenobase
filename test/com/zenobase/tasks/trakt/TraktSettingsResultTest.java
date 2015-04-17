package com.zenobase.tasks.trakt;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.tasks.ResultTestSupport;

public class TraktSettingsResultTest extends ResultTestSupport {

	@Test
	public void test() {
		TraktSettingsResult result = new TraktSettingsResult(readObject("TraktSettingsResultTest.json"));
		assertThat(result.getUsername()).isEqualTo("justin");
		assertThat(result.getTimeZone()).isEqualTo(DateTimeZone.forID("America/Los_Angeles"));
	}
}
