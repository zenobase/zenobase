package controllers;

import java.util.Arrays;

import com.google.common.base.Objects;

public class SearchForm {

	public String[] w;
	public String[] q;

	public String[] getW() {
		return w;
	}

	public void setW(String[] w) {
		this.w = w;
	}

	public String[] getQ() {
		return q;
	}

	public void setQ(String[] q) {
		this.q = q;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("w", Arrays.toString(w))
			.add("q", Arrays.toString(q)).toString();
	}
}
