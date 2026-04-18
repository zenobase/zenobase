package com.zenobase.tasks.withings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.TokenField;
import com.zenobase.json.UnitField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;
import javax.measure.quantity.Temperature;
import javax.measure.unit.Unit;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class WithingsTemperatureTask extends Task {

	public static final String TYPE = "withings-temperature";
	public static final TokenField TAG = new TokenField("tag");
	public static final UnitField<Temperature> UNIT = new UnitField<>("unit");
	public static final TokenField TIMEZONE = new TokenField("timezone");

	public WithingsTemperatureTask(ObjectNode node) {
		super(node);
	}

	WithingsTemperatureTask(
		String bucketId,
		Identity principal,
		String tag,
		Unit<Temperature> unit,
		DateTimeZone timezone,
		@Nullable String marker
	) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		setSetting(UNIT, unit);
		setSetting(TIMEZONE, timezone.getID());
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	public @Nullable Unit<Temperature> getUnit() {
		return getSetting(UNIT);
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	@Override
	public WithingsTemperatureTask copy() {
		return copy(getClass());
	}
}
