package com.zenobase.tasks.bodymedia;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.LocalDate;
import org.junit.Test;

import com.zenobase.tasks.ResultTestSupport;

public class BodyMediaTimezonesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		BodyMediaTimezonesResult result = new BodyMediaTimezonesResult(readObject("BodyMediaTimezonesResultTest.json"));
		assertThat(result.getTimezoneMap().getBegin()).isEqualTo(LocalDate.parse("2012-12-30"));
	}
}
