package com.zenobase.tasks.moves;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MovesLocateTask extends Task {

	public static final String TYPE = "moves-locate";

	public MovesLocateTask(ObjectNode node) {
		super(node);
	}

	public MovesLocateTask(String bucketId, Identity principal) {
		this(bucketId, principal, null);
	}

	MovesLocateTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	public DateTime getFrom() {
		String marker = getMarker();
		return marker != null
			? DateTime.parse(marker, ISODateTimeFormat.dateTime().withOffsetParsed())
			: null;
	}

	@Override
	public MovesLocateTask copy() {
		return copy(getClass());
	}
}
