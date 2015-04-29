package com.zenobase.tasks.moodpanda;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import com.zenobase.tasks.ResultTestSupport;

public class MoodPandaUserResultTest extends ResultTestSupport {

	@Test
	public void test() {

		MoodPandaUserResult result = new MoodPandaUserResult(readXml("MoodPandaUserResultTest.xml"));
		assertThat(result.getUserId()).isEqualTo("7510103");
		assertThat(result.getOffset()).isEqualTo(-7.5);
	}
}
