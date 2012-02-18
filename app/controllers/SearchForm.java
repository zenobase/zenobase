package controllers;

import com.google.common.base.Objects;

public class SearchForm {

	public String w = "";
	public String q = "";

	public String getW() {
		return w;
	}

	public String[] getWidgets() {
		return w.length() > 0 ? w.split(";") : new String[0];
	}

	public void setW(String w) {
		this.w = w;
	}

	public String getQ() {
		return q;
	}

	public String[] getQueries() {
		return q.length() > 0 ? q.split(";") : new String[0];
	}

	public void setQ(String q) {
		this.q = q;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("w", w).add("q", q).toString();
	}
}
