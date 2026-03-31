package com.zenobase.tasks.reporter;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import com.zenobase.tasks.ResultTestSupport;

public class ConfigurationResultTest extends ResultTestSupport {

	@Test
	public void test() {

		Question q1 = new Question("How are you 0-10?", "Mood", "rating");
		Question q2 = new Question("Pick a color", "Color", null);
		Question q3 = new Question("What are you doing?", null, null);
		ConfigurationResult result = new ConfigurationResult(readObject("ConfigurationResultTest.json"));
		Configuration config = result.get();

		assertThat(config.getTimezone()).isEqualTo(DateTimeZone.forID("America/New_York"));
		assertThat(config.getQuestion(q1.prompt())).isEqualTo(q1);
		assertThat(config.getQuestion(q2.prompt())).isEqualTo(q2);
		assertThat(config.getQuestion(q3.prompt())).isEqualTo(q3);
		assertThat(config.getQuestion("Huh?")).isNull();
	}
}
