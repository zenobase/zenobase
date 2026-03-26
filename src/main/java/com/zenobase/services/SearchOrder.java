package com.zenobase.services;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch.core.SearchRequest;

import com.zenobase.json.Schema;

public class SearchOrder {

	private final String field;
	private final boolean asc;

	public SearchOrder(String field, boolean asc) {
		this.field = field;
		this.asc = asc;
	}

	public void apply(SearchRequest.Builder builder) {
		builder.sort(s -> s.field(f -> f.field(field).order(asc ? SortOrder.Asc : SortOrder.Desc)));
	}

	public SearchOrder reverse() {
		return new SearchOrder(field, !asc);
	}

	public static SearchOrder valueOf(String s, Schema schema) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(s));
		boolean asc = true;
		int sign = s.charAt(0);
		if (sign == '-') {
			asc = false;
			s = s.substring(1);
		}
		return new SearchOrder(schema.getField(s).getPathForSorting(), asc);
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof SearchOrder && equals((SearchOrder) that);
	}

	private boolean equals(SearchOrder that) {
		return field.equals(that.field) && asc == that.asc;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(field, asc);
	}

	@Override
	public String toString() {
		return asc ? field : '-' + field;
	}
}
