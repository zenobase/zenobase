package com.zenobase.tasks.reporter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.LocalDate;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class ReporterTask extends Task {

	public static final String TYPE = "reporter-questions";
	public static final TokenField FOLDER = new TokenField("folder");

	public ReporterTask(ObjectNode node) {
		super(node);
	}

	public ReporterTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	ReporterTask(String bucketId, Identity principal, String folder) {
		super(TYPE, bucketId, principal);
		setSetting(FOLDER, folder);
	}

	public String getFolder() {
		return getSetting(FOLDER);
	}

	public LocalDate getFirstDate() {
		String marker = getMarker();
		return marker != null ? LocalDate.parse(marker) : null;
	}

	@Override
	public ReporterTask copy() {
		return copy(getClass());
	}
}
