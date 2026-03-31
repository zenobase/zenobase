package com.zenobase.tasks.reporter;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class Configuration {

	private DateTimeZone timezone = DateTimeZone.UTC;
	private final Map<String, Question> questions = Maps.newHashMap();

	public DateTimeZone getTimezone() {
		return timezone;
	}

	public void setTimezone(DateTimeZone timezone) {
		this.timezone = Preconditions.checkNotNull(timezone);
	}

	public @Nullable Question getQuestion(String prompt) {
		return questions.get(prompt);
	}

	public void addQuestion(Question question) {
		questions.put(question.prompt(), question);
	}

	public boolean valid() {
		return !questions.isEmpty();
	}
}
