package com.zenobase.common;

import org.joda.time.ReadablePartial;

public class LocalDateTimeRangeParser extends RangeParser<ReadablePartial> {

	@Override
	protected ReadablePartial getValue(String s) {
		return LocalDateTimeFormat.parse(s);
	}
}
