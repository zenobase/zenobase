package com.zenobase.jobs;

import java.util.Locale;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.opensearch.client.opensearch.snapshot.SnapshotInfo;

import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.DurationField;
import com.zenobase.json.TokenField;

public class Snapshot extends DomainNode {

	private static final TokenField ID = new TokenField("@id");
	private static final TokenField STATE = new TokenField("state");
	private static final DateTimeField CREATED = new DateTimeField("created");
	private static final DurationField DURATION = new DurationField("duration");

	public Snapshot(SnapshotInfo info) {
		setValue(ID, info.snapshot());
		setValue(
				STATE,
				info.state() != null ? info.state().toLowerCase(Locale.ROOT).replace('_', ' ') : "unknown");
		long startTime = info.startTimeInMillis() != null ? info.startTimeInMillis() : 0;
		setValue(CREATED, new DateTime(startTime, DateTimeZone.UTC));
		long endTime = info.endTimeInMillis() != null ? info.endTimeInMillis() : 0;
		if (endTime > 0) {
			setValue(DURATION, Duration.millis(endTime - startTime));
		}
	}

	public Snapshot(ObjectNode node) {
		super(node);
	}
}
