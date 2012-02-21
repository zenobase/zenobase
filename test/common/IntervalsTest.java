package common;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.joda.time.Years;
import org.junit.Test;

public class IntervalsTest {

	@Test
	public void testForYear() {
		Interval year = Intervals.forYear(Years.years(2012), DateTimeZone.UTC);
		Assert.assertEquals(new DateTime(2012, 1, 1, 0, 0, DateTimeZone.UTC), year.getStart()); 
		Assert.assertEquals(new DateTime(2013, 1, 1, 0, 0, DateTimeZone.UTC), year.getEnd()); 
	}

	@Test
	public void testForMonth() {
		Interval month = Intervals.forMonth(new YearMonth(2012, 1), DateTimeZone.UTC);
		Assert.assertEquals(new DateTime(2012, 1, 1, 0, 0, DateTimeZone.UTC), month.getStart()); 
		Assert.assertEquals(new DateTime(2012, 2, 1, 0, 0, DateTimeZone.UTC), month.getEnd()); 
	}

	@Test
	public void testForDay() {
		Interval day = Intervals.forDay(new LocalDate(2012, 1, 1), DateTimeZone.UTC);
		Assert.assertEquals(new DateTime(2012, 1, 1, 0, 0, DateTimeZone.UTC), day.getStart()); 
		Assert.assertEquals(new DateTime(2012, 1, 2, 0, 0, DateTimeZone.UTC), day.getEnd()); 
	}
}
