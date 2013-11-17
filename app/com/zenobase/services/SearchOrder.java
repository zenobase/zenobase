package com.zenobase.services;

import java.util.Objects;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class SearchOrder {

	private final String field;
	private final boolean asc;

	public SearchOrder(String field, boolean asc) {
		this.field = field;
		this.asc = asc;
	}

	public void apply(SearchSourceBuilder search) {
		search.sort(field, asc ? SortOrder.ASC : SortOrder.DESC);
	}

	public static SearchOrder valueOf(String s) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(s));
		boolean asc = true;
		int sign = s.charAt(0);
		if (sign == '-') {
			asc = false;
			s = s.substring(1);
		}
		return new SearchOrder(s, asc);
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof SearchOrder
			&& equals((SearchOrder) that);
	}

	private boolean equals(SearchOrder that) {
		return field.equals(that.field)
			&& asc == that.asc;
	}

	@Override
	public int hashCode() {
		return Objects.hash(field, asc);
	}

	@Override
	public String toString() {
		return asc ? field : '-' + field;
	}
}
