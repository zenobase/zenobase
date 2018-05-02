package com.zenobase.tasks.beddit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class BedditTask extends Task {

	public static final String TYPE = "beddit-sleep";
	public static final TokenField TAG = new TokenField("tag");

	public BedditTask(ObjectNode node) {
		super(node);
	}

	public BedditTask(String bucketId, Identity principal, String tag) {
		super(TYPE, bucketId, principal);
		setSetting(TAG, tag);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public DateTime getFrom() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker, ISODateTimeFormat.dateTime().withOffsetParsed()) : new DateTime(0);
	}

	@Override
	public BedditTask copy() {
		return copy(getClass());
	}
}
