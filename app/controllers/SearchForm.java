package controllers;

import java.util.Arrays;

import com.google.common.base.Objects;

public class SearchForm {
	public int offset;
	public int limit = 1;
	public String[] facet;
	public String[] filter;
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
	public int getLimit() {
		return limit;
	}
	public void setLimit(int limit) {
		this.limit = limit;
	}
	public String[] getFacet() {
		return facet;
	}
	public void setFacet(String[] facet) {
		this.facet = facet;
	}
	public String[] getFilter() {
		return filter;
	}
	public void setFilter(String[] filter) {
		this.filter = filter;
	}
	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("offset", offset).add("limit", limit)
				.add("facet", Arrays.toString(facet))
				.add("filter", Arrays.toString(filter))
				.toString();
	}
}