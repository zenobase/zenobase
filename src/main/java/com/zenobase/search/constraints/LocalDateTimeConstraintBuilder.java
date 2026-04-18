package com.zenobase.search.constraints;

import com.zenobase.common.LocalInterval;
import com.zenobase.common.LocalIntervals;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;

public class LocalDateTimeConstraintBuilder extends ConstraintBuilder {

	public LocalDateTimeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public @Nullable Query build(String value) {
		return build(LocalIntervals.valueOf(value));
	}

	private @Nullable Query build(@Nullable LocalInterval interval) {
		if (interval == null) {
			return null;
		} else if (interval.toDurationMillis() > 1L) {
			String gte = toString(interval.start());
			String lt = toString(interval.end());
			return Query.of(q -> q.range(r -> r.field(getPath()).gte(JsonData.of(gte)).lt(JsonData.of(lt))));
		} else {
			String val = toString(interval.start());
			return Query.of(q -> q.term(t -> t.field(getPath()).value(FieldValue.of(val))));
		}
	}

	private static String toString(LocalDateTime value) {
		return value.toDateTime(DateTimeZone.UTC).toString();
	}
}
