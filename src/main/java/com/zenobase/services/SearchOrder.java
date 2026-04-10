package com.zenobase.services;

import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch.core.SearchRequest;

import com.zenobase.json.Field;
import com.zenobase.json.Schema;

public class SearchOrder {

	private final String field;
	private final boolean asc;

	public SearchOrder(String field, boolean asc) {
		this.field = field;
		this.asc = asc;
	}

	public SearchOrder thenBy(String field, boolean asc) {
		return new CompoundSearchOrder(this, new SearchOrder(field, asc));
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
		var field = schema.getField(s);
		Preconditions.checkArgument(field != null, "unknown field: %s", s);
		return new SearchOrder(field.getPathForSorting(), asc);
	}

	public static SearchOrder asc(Field<?>... fields) {
		Preconditions.checkArgument(fields.length > 0);
		var order = new SearchOrder(fields[0].getName(), true);
		for (int i = 1; i < fields.length; ++i) {
			order = order.thenBy(fields[i].getName(), true);
		}
		return order;
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof SearchOrder o && field.equals(o.field) && asc == o.asc;
	}

	@Override
	public int hashCode() {
		return Objects.hash(field, asc);
	}

	@Override
	public String toString() {
		return asc ? field : '-' + field;
	}

	private static class CompoundSearchOrder extends SearchOrder {

		private final SearchOrder first;
		private final SearchOrder second;

		CompoundSearchOrder(SearchOrder first, SearchOrder second) {
			super(first.field, first.asc);
			this.first = first;
			this.second = second;
		}

		@Override
		public void apply(SearchRequest.Builder builder) {
			first.apply(builder);
			second.apply(builder);
		}
	}
}
