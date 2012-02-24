package common;

import static org.hamcrest.Matchers.equalTo;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.joda.time.Years;
import org.junit.Assert;
import org.junit.Test;

public class IntervalsTest {

	@Test
	public void testForYear() {
		Interval year = Intervals.forYear(Years.years(2012), DateTimeZone.UTC);
		Assert.assertThat(year.getStart(), equalTo(new DateTime(2012, 1, 1, 0, 0, DateTimeZone.UTC))); 
		Assert.assertThat(year.getEnd(), equalTo(new DateTime(2013, 1, 1, 0, 0, DateTimeZone.UTC))); 
	}

	@Test
	public void testForMonth() {
		Interval month = Intervals.forMonth(new YearMonth(2012, 1), DateTimeZone.UTC);
		Assert.assertThat(month.getStart(), equalTo(new DateTime(2012, 1, 1, 0, 0, DateTimeZone.UTC))); 
		Assert.assertThat(month.getEnd(), equalTo(new DateTime(2012, 2, 1, 0, 0, DateTimeZone.UTC))); 
	}

	@Test
	public void testForDay() {
		Interval day = Intervals.forDay(new LocalDate(2012, 1, 1), DateTimeZone.UTC);
		Assert.assertThat(day.getStart(), equalTo(new DateTime(2012, 1, 1, 0, 0, DateTimeZone.UTC))); 
		Assert.assertThat(day.getEnd(), equalTo(new DateTime(2012, 1, 2, 0, 0, DateTimeZone.UTC))); 
	}
}
