package com.zenobase.common;

import com.zenobase.json.IntegerField;
import java.util.List;

public interface PartialList<T> extends List<T> {
	IntegerField TOTAL = new IntegerField("total");

	long getTotal();
}
