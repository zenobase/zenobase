package com.zenobase.tasks.google;

import java.util.Arrays;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class DataPoint {

	private final DateTime begin;
	private final DateTime end;
	private final String dataType;
	private final String origin;
	private final Object[] values;

	public DataPoint(DateTime begin, DateTime end, String dataType, String origin, Object[] values) {
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

	public Object getValue(int i) {
		return values[i];
	}

	public <T> T getValue(int i, Class<T> type) {
		return type.isInstance(values[i]) ? type.cast(values[i]) : null;
	}

	@Override
	public String toString() {
		String s = String.format("%s..%s %s %s", begin, end, dataType, Arrays.toString(values));
		if (origin != null) {
			s += " <- " + origin;
		}
		return s;
	}
}
