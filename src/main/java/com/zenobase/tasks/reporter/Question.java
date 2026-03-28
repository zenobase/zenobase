package com.zenobase.tasks.reporter;

import java.util.Objects;

import com.google.common.base.Preconditions;

public class Question {

	private final String prompt;
	private final String tag;
	private final String field;

	public Question(String prompt, String tag, String field) {
		this.prompt = Preconditions.checkNotNull(prompt);
		this.tag = tag;
		this.field = field;
	}

	public String getPrompt() {
		return prompt;
	}

	public String getTag() {
		return tag;
	}

	public String getField() {
		return field;
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Question q
				&& Objects.equals(prompt, q.prompt)
				&& Objects.equals(tag, q.tag)
				&& Objects.equals(field, q.field);
	}

	@Override
	public int hashCode() {
		return Objects.hash(prompt, tag, field);
	}

	@Override
	public String toString() {
		return prompt;
	}
}
