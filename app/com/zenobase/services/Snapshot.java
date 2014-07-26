package com.zenobase.services;

import org.elasticsearch.snapshots.SnapshotInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
		setValue(ID, info.name());
		setValue(STATE, info.state().name());
		setValue(CREATED, new DateTime(info.startTime(), DateTimeZone.UTC));
		if (info.endTime() > 0) {
			setValue(DURATION, Duration.millis(info.endTime() - info.startTime()));
		}
	}

	public Snapshot(ObjectNode node) {
		super(node);
	}
}
