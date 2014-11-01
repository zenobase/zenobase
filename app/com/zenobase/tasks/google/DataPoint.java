package com.zenobase.tasks.google;

import java.math.BigDecimal;
import java.util.Arrays;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class DataPoint {

	private final DateTime begin;
	private final DateTime end;
	private final String dataType;
	private final String origin;
	private final BigDecimal[] values;

	public DataPoint(DateTime begin, DateTime end, String dataType, String origin, BigDecimal[] values) {
		this.begin = begin;
		this.end = end;
		this.dataType = dataType;
		this.origin = origin;
		this.values = values;
	}

	public DateTime getBegin() {
		return begin;
	}

	public DateTime getEnd() {
		return end;
	}

	public Duration getDuration() {
		return new Duration(begin, end);
	}

	public boolean isRange() {
		return !begin.equals(end);
	}

	public String getDataType() {
		return dataType;
	}

	public String getOrigin() {
		return origin;
	}

	public BigDecimal getValue(int i) {
		return values[i];
	}

	@Override
	public String toString() {
		return String.format("%s..%s %s %s", begin, end, dataType, Arrays.toString(values));
	}
}
