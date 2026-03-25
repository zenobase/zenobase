package com.zenobase.tasks.reporter;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTimeZone;

public class ConfigurationResult {

	private final JsonNode node;

	public ConfigurationResult(JsonNode node) {
		this.node = node;
	}

	public Configuration get() {
		var config = new Configuration();
		String timezone = node.path("timezone").textValue();
		if (timezone != null) {
			config.setTimezone(DateTimeZone.forID(timezone));
		}
		for (JsonNode questionNode : node.path("questions")) {
			String prompt = questionNode.path("prompt").textValue();
			String tag = questionNode.path("tag").textValue();
			String field = questionNode.path("field").textValue();
			config.addQuestion(new Question(prompt, tag, field));
		}
		Preconditions.checkState(config.valid(), "Can't parse config: %s", node);
		return config;
	}
}
