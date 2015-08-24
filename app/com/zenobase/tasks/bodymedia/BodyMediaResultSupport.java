package com.zenobase.tasks.bodymedia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

abstract class BodyMediaResultSupport {

	static final Resource SOURCE = new Resource("BodyMedia", "http://bodymedia.com/");
	static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZZ").withOffsetParsed();
	static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("yyyyMMdd");

	private final ObjectNode node;
	private final Identity author;
	private final DateTime lastSync;

	protected BodyMediaResultSupport(ObjectNode node, Identity author) {
		this.node = Preconditions.checkNotNull(node);
		this.author = Preconditions.checkNotNull(author);
		this.lastSync = Preconditions.checkNotNull(getDateTime(node.path("lastSync").path("dateTime")));
	}

	protected JsonNode path(String path) {
		return node.path(path);
	}

	protected Identity getAuthor() {
		return author;
	}

	protected DateTime getLastSyncDate() {
		return lastSync;
	}

	protected static DateTime getDateTime(JsonNode node) {
		Preconditions.checkArgument(!node.isMissingNode());
		return DateTime.parse(node.textValue(), DATE_TIME_FORMAT);
	}

	protected static LocalDate getLocalDate(JsonNode node) {
		Preconditions.checkArgument(!node.isMissingNode());
		return LocalDate.parse(node.textValue(), DATE_FORMAT);
	}
}
