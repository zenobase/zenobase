package com.zenobase.tasks.microsoft;

import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

abstract class MicrosoftHealthResultSupport {

	static final Resource SOURCE = new Resource("Microsoft Health", "https://www.microsoft.com/microsoft-health/");

	protected final JsonNode node;
	protected final Identity author;
	protected final DateTimeZone zone;

	public MicrosoftHealthResultSupport(JsonNode node, Identity author, DateTimeZone zone) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.zone = zone;
	}

	public String next() {
		return node.path("nextPage").textValue();
	}

	protected DateTime dateTimeValue(JsonNode node) {
		return DateTime.parse(node.textValue()).withZoneRetainFields(zone);
	}
}
