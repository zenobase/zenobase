package com.zenobase.search;

import java.util.Objects;
import javax.measure.unit.Unit;

import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import com.zenobase.common.Units;
import com.zenobase.models.Event;

public class TimelineFacet {

	public static final String TYPE = "timeline";

	public static FacetBuilder builder(FilterParser filterParser) {

		return new FacetBuilder() {

			@Override
			public Facet build(FacetOptions options) {
				String id = Objects.requireNonNull(options.get("id"));
				String keyField =
						Objects.requireNonNull(options.get("key_field", String.class, Event.TIMESTAMP.getName()));
				String valueField =
						Objects.requireNonNull(options.get("field", String.class, Event.TIMESTAMP.getName()));
				Unit<?> unit = getUnit(options.get("unit"));
				String interval = options.get("interval", String.class, "month");
				String range = options.get("range");
				DateTimeZone timezone = options.get("timezone", DateTimeZone.class, null);
				Query filter = filterParser.parse(options.get("filter"));
				return timezone != null
						? new OffsetTimelineFacet(id, keyField, valueField, interval, range, timezone, unit, filter)
						: new LocalTimelineFacet(
								id,
								local(keyField),
								valueField.equals(keyField) ? local(valueField) : valueField,
								interval,
								range,
								unit,
								filter);
			}

			private Unit<?> getUnit(@Nullable String value) {
				return value != null ? Units.valueOf(value) : Unit.ONE;
			}

			private String local(String field) {
				return "$" + field + ".time";
			}
		};
	}
}
