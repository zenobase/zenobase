package com.zenobase.tasks.google;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import com.zenobase.tasks.ResultTestSupport;

public class DatasetResultTest extends ResultTestSupport {

	private static final DateTimeZone TIMEZONE = DateTimeZone.forID("America/Los_Angeles");

	@Test
	public void test() {

		DatasetResult result = new DatasetResult(readObject("DatasetResultTest.json"), TIMEZONE, dateTime("2014-10-29T00:17:46.474-07:00"));
		List<DataPoint> points = result.getDataPoints();
		assertThat(points).hasSize(2);

		assertThat(points.get(0).getBegin()).isEqualTo(dateTime("2014-10-29T09:31:45.945-07:00"));
		assertThat(points.get(0).getEnd()).isEqualTo(dateTime("2014-10-29T09:32:45.946-07:00"));
		assertThat(points.get(0).getDataType()).isEqualTo("com.google.step_count.delta");
		assertThat(points.get(0).getValue(0)).isEqualTo(new BigDecimal(30));

		assertThat(points.get(1).getBegin()).isEqualTo(dateTime("2014-10-29T10:57:02.268-07:00"));
		assertThat(points.get(1).getEnd()).isEqualTo(dateTime("2014-10-29T10:58:02.268-07:00"));
		assertThat(points.get(1).getDataType()).isEqualTo("com.google.step_count.delta");
		assertThat(points.get(1).getValue(0)).isEqualTo(new BigDecimal(2));
	}

	protected static DateTime dateTime(String value) {
		return DateTime.parse(value, ISODateTimeFormat.dateTime().withZone(TIMEZONE));
	}
}
