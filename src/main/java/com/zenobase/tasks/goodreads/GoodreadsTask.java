package com.zenobase.tasks.goodreads;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class GoodreadsTask extends Task {

	public static final String TYPE = "goodreads";
	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField SHELF = new TokenField("shelf");

	public GoodreadsTask(ObjectNode node) {
		super(node);
	}

	public GoodreadsTask(String bucketId, Identity principal, @Nullable String marker, String tag, String shelf) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		setSetting(SHELF, shelf);
	}

	public String getTag() {
		return Objects.requireNonNull(getSetting(TAG));
	}

	public @Nullable String getShelf() {
		return getSetting(SHELF);
	}

	@Override
	public GoodreadsTask copy() {
		return copy(getClass());
	}
}
