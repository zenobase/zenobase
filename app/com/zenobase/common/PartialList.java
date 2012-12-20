package com.zenobase.common;

import java.util.List;

import com.zenobase.json.IntegerField;

public interface PartialList<T> extends List<T> {

	final IntegerField TOTAL = new IntegerField("total");

	long getTotal();
}
