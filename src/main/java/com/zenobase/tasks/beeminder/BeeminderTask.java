package com.zenobase.tasks.beeminder;

import java.util.Objects;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.Units;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class BeeminderTask extends Task {

	public static final String TYPE = "beeminder";
	public static final TokenField GOAL = new TokenField("goal");
	public static final TokenField FILTER = new TokenField("filter");
	public static final TokenField KEY_FIELD = new TokenField("key_field");
	public static final TokenField FIELD = new TokenField("field");
	public static final TokenField UNIT = new TokenField("unit");

	public BeeminderTask(ObjectNode node) {
		super(node);
	}

	public BeeminderTask(
			String bucketId,
			Identity principal,
			String goal,
			String filter,
			String keyField,
			String field,
			String unit,
			String marker) {
		super(TYPE, bucketId, principal);
		setSetting(GOAL, Preconditions.checkNotNull(goal));
		setSetting(KEY_FIELD, Preconditions.checkNotNull(keyField));
		setSetting(FILTER, filter);
		setSetting(FIELD, field);
		setSetting(UNIT, unit);
		setMarker(Preconditions.checkNotNull(marker));
	}

	public DateTime getFrom() {
		return DateTime.parse(getMarker(), ISODateTimeFormat.dateTime().withOffsetParsed());
	}

	public String getGoal() {
		return Objects.requireNonNull(getSetting(GOAL));
	}

	public String getKeyField() {
		return Objects.requireNonNull(getSetting(KEY_FIELD));
	}

	public @Nullable String getFilter() {
		return Strings.emptyToNull(getSetting(FILTER));
	}

	public @Nullable String getField() {
		return getSetting(FIELD);
	}

	public Unit<?> getUnit() {
		String value = getSetting(UNIT);
		return value != null ? Units.valueOf(value) : Unit.ONE;
	}

	@Override
	public BeeminderTask copy() {
		return copy(getClass());
	}
}
