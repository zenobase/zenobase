package com.zenobase.search;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.CalendarInterval;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramBucket;
import org.opensearch.client.opensearch._types.aggregations.StatsAggregate;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import com.zenobase.common.OffsetIntervals;
import com.zenobase.json.Nodes;

public class OffsetTimelineFacet extends TimelineFacetSupport {

	private final @Nullable String interval;
	private final @Nullable Interval range;
	private final DateTimeZone timezone;

	public OffsetTimelineFacet(
			String id,
			String keyField,
			String valueField,
			@Nullable String interval,
			@Nullable String range,
			DateTimeZone timezone,
			Unit<?> unit,
			@Nullable Query filter) {
		super(id, keyField, valueField, unit, filter);
		this.interval = interval;
		this.range = !Strings.isNullOrEmpty(range) ? OffsetIntervals.valueOf(range) : null;
		this.timezone = timezone;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		CalendarInterval calendarInterval = DateHistograms.parseInterval(Objects.requireNonNull(interval));
		String tz = timezone.toTimeZone().toZoneId().getId();
		String f = getField();
		Aggregation aggregation = Aggregation.of(a -> a.dateHistogram(dh ->
						dh.field(keyField).calendarInterval(calendarInterval).timeZone(tz))
				.aggregations(getId(), Aggregation.of(sa -> sa.stats(s -> s.field(f)))));
		addAggregation(getId(), aggregation, builder);
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		Aggregate aggregate = getAggregate(response);
		if (aggregate == null) {
			return Nodes.newArray();
		}
		List<DateHistogramBucket> buckets = aggregate.dateHistogram().buckets().array();
		Map<String, ObjectNode> counts = Collections.emptyMap();
		if (!buckets.isEmpty()) {
			counts = getMap(getInterval(buckets));
			for (DateHistogramBucket bucket : buckets) {
				if (bucket.docCount() > 0) {
					long bucketTime = bucket.key();
					String key = getLabel(toDateTime(bucketTime));
					if (range == null || counts.containsKey(key)) {
						ObjectNode entryNode = MoreObjects.firstNonNull(counts.get(key), Nodes.newObject());
						entryNode.put("label", key);
						entryNode.put("time", addOffset(bucketTime));
						entryNode.put("count", bucket.docCount());
						if (!keyField.equals(valueField)) {
							Aggregate bucketAggregate = bucket.aggregations().get(getId());
							if (bucketAggregate == null) {
								continue;
							}
							StatsAggregate stats = bucketAggregate.stats();
							addValue(entryNode, "min", stats.min());
							addValue(entryNode, "max", stats.max());
							addValue(entryNode, "sum", stats.sum());
							addValue(entryNode, "avg", stats.avg());
						}
						counts.put(key, entryNode);
					}
				}
			}
		}
		return toJson(counts.values());
	}

	private long addOffset(long time) {
		return time + (timezone != null ? timezone.getOffset(time) : 0);
	}

	private @Nullable Interval getInterval(List<DateHistogramBucket> buckets) {
		if (range != null) {
			return range;
		}
		long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
		for (DateHistogramBucket bucket : buckets) {
			if (bucket.docCount() > 0) {
				long bucketTime = bucket.key();
				min = Math.min(min, bucketTime);
				max = Math.max(max, bucketTime);
			}
		}
		return min <= max ? new Interval(toDateTime(min), toDateTime(max)) : null;
	}

	private DateTime toDateTime(long time) {
		return new DateTime(time, timezone);
	}

	private Map<String, ObjectNode> getMap(@Nullable Interval interval) {
		Map<String, ObjectNode> counts = Maps.newTreeMap();
		if (interval != null) {
			for (DateTime time : OffsetIntervals.expand(interval.getStart(), interval.getEnd(), this.interval)) {
				String label = getLabel(time);
				ObjectNode node = Nodes.newObject();
				node.put("label", label);
				node.put("time", addOffset(time.getMillis()));
				node.put("count", 0);
				counts.put(label, node);
			}
		}
		return counts;
	}

	private String getLabel(DateTime time) {
		return OffsetIntervals.toString(time, interval);
	}
}
