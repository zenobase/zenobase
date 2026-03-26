package com.zenobase.tasks.runkeeper;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

abstract class RunkeeperResultSupport {

	static final DateTimeFormatter TIME_FORMAT = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss");

	protected final JsonNode node;
	protected final Identity author;
	protected final DateTimeZone timezone;

	public RunkeeperResultSupport(JsonNode node, Identity author, DateTimeZone timezone) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.timezone = timezone;
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode itemNode : node.path("items")) {
			events.add(newEvent(itemNode));
		}
		return events;
	}

	protected abstract Event newEvent(JsonNode node);

	protected DateTime dateTimeValue(JsonNode node) {
		return dateTimeValue(node, timezone);
	}

	protected DateTime dateTimeValue(JsonNode node, DateTimeZone timezone) {
		LocalDateTime local = TIME_FORMAT.parseLocalDateTime(node.textValue());
		Preconditions.checkState(!timezone.isLocalDateTimeGap(local), "<%s> does not exist in <%s>", local, timezone);
		return local.toDateTime(timezone);
	}

	protected @Nullable Resource resourceValue(JsonNode node) {
		return node.isTextual() ? new Resource("RunKeeper", node.textValue()) : null;
	}

	public String getNext() {
		return node.path("next").textValue();
	}

	protected static boolean isZero(JsonNode node) {
		Preconditions.checkArgument(node.isMissingNode() || node.isNull() || node.isNumber());
		return node.doubleValue() == 0.0;
	}
}
