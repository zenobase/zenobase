package com.zenobase.tasks.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import com.zenobase.tasks.ResultTestSupport;

public class DatasetResultTest extends ResultTestSupport {

	private static final DateTimeZone TIMEZONE = DateTimeZone.forID("America/Los_Angeles");

	@Test
	public void test() {

		DatasetResult result = new DatasetResult(readObject("DatasetResultTest.json"), TIMEZONE);
		assertThat(result.getNextPageToken()).isEqualTo("xxx");
		List<DataPoint> points = result.getDataPoints();
		assertThat(points).hasSize(3);

		assertThat(points.get(0).getBegin()).isEqualTo(dateTime("2014-10-29T09:31:45.945-07:00"));
		assertThat(points.get(0).getEnd()).isEqualTo(dateTime("2014-10-29T09:32:45.946-07:00"));
		assertThat(points.get(0).getDataType()).isEqualTo("com.google.step_count.delta");
		assertThat(points.get(0).getValue(0)).isEqualTo(new BigDecimal(30));

		assertThat(points.get(1).getBegin()).isEqualTo(dateTime("2014-10-29T10:57:02.268-07:00"));
		assertThat(points.get(1).getEnd()).isEqualTo(dateTime("2014-10-29T10:58:02.268-07:00"));
		assertThat(points.get(1).getDataType()).isEqualTo("com.google.step_count.delta");
		assertThat(points.get(1).getValue(0)).isEqualTo(new BigDecimal(2));

		assertThat(points.get(2).getBegin()).isEqualTo(dateTime("2016-06-02T15:17:38.000-07:00"));
		assertThat(points.get(2).getEnd()).isEqualTo(dateTime("2016-06-02T15:17:40.000-07:00"));
		assertThat(points.get(2).getDataType()).isEqualTo("com.google.nutrition");
		assertThat(points.get(2).getValue(0, Map.class)).contains(
			entry("protein", new BigDecimal("5.32574987411499")),
			entry("calories", new BigDecimal("153.4949951171875")),
			entry("sugar", new BigDecimal("0.40095001459121704"))
		);
		assertThat(points.get(2).getValue(1, BigDecimal.class)).isEqualTo(new BigDecimal(1));
		assertThat(points.get(2).getValue(2, String.class)).isEqualTo("Oatmeal");
	}

	protected static DateTime dateTime(String value) {
		return DateTime.parse(value, ISODateTimeFormat.dateTime().withZone(TIMEZONE));
	}
}
