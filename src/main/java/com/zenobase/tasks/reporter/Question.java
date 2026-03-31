package com.zenobase.tasks.reporter;

import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

public record Question(
		String prompt, @Nullable String tag, @Nullable String field) {

	public Question {
		Preconditions.checkNotNull(prompt);
	}

	@Override
	public String toString() {
		return prompt;
	}
}
