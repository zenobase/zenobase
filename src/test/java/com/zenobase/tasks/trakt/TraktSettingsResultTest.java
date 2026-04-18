package com.zenobase.tasks.trakt;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.tasks.ResultTestSupport;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class TraktSettingsResultTest extends ResultTestSupport {

	@Test
	public void test() {
		TraktSettingsResult result = new TraktSettingsResult(readObject("TraktSettingsResultTest.json"));
		assertThat(result.getTimeZone()).isEqualTo(DateTimeZone.forID("America/Los_Angeles"));
	}
}
