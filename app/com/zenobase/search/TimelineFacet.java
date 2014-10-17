package com.zenobase.search;

import javax.measure.unit.Unit;

import org.elasticsearch.index.query.FilterBuilder;
import org.joda.time.DateTimeZone;

import com.zenobase.common.Units;
import com.zenobase.models.Event;

public class TimelineFacet {

	public static final String TYPE = "timeline";

	public static FacetBuilder builder(final FilterParser filterParser) {

		return new FacetBuilder() {

			@Override
			public Facet build(FacetOptions options) {
				String id = options.get("id");
				String keyField = options.get("key_field", String.class, Event.TIMESTAMP.getName());
				String valueField = options.get("field", String.class, Event.TIMESTAMP.getName());
				Unit<?> unit = getUnit(options.get("unit"));
				String interval = options.get("interval", String.class, "month");
				String range = options.get("range");
				DateTimeZone timezone = options.get("timezone", DateTimeZone.class, null);
				FilterBuilder filter = filterParser.parse(options.get("filter"));
				return timezone != null
					? new OffsetTimelineFacet(id, keyField, valueField, interval, range, timezone, unit, filter)
					: new LocalTimelineFacet(id, keyField, valueField, interval, range, unit, filter);
			}

			private Unit<?> getUnit(String value) {
				return value != null ? Units.valueOf(value) : Unit.ONE;
			}
		};
	}
}
