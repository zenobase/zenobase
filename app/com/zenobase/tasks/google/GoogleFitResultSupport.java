package com.zenobase.tasks.google;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

import com.zenobase.models.Resource;

abstract class GoogleFitResultSupport {

	protected final JsonNode node;
	protected final DateTimeZone zone;

	public GoogleFitResultSupport(JsonNode node, DateTimeZone zone) {
		this.node = node;
		this.zone = zone;
	}

	protected String activityTypeValue(JsonNode node) {
		return node.isInt() ? ActivityTypes.forID(node.intValue()) : null;
	}

	protected DateTime dateTimeValue(JsonNode node) {
		long value = node.asLong();
		Preconditions.checkArgument(value != 0L, "Can't find timestamp: %s", node);
		return new DateTime(value, zone);
	}

	protected Resource resourceValue(JsonNode node) {
		String title = node.path("name").textValue();
		String url = node.path("detailsUrl").textValue();
		return title != null && url != null ? new Resource(title, url) : null;
	}
}
