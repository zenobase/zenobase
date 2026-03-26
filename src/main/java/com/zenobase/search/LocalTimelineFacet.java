package com.zenobase.search;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.CalendarInterval;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramBucket;
import org.opensearch.client.opensearch._types.aggregations.StatsAggregate;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import com.zenobase.common.LocalInterval;
import com.zenobase.common.LocalIntervals;
import com.zenobase.json.Nodes;

public class LocalTimelineFacet extends TimelineFacetSupport {

	private final String interval;
	private final LocalInterval range;

	public LocalTimelineFacet(
			String id, String keyField, String valueField, String interval, String range, Unit<?> unit, Query filter) {
		super(id, keyField, valueField, unit, filter);
		this.interval = interval;
		this.range = !Strings.isNullOrEmpty(range) ? LocalIntervals.valueOf(range) : null;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		CalendarInterval calendarInterval = DateHistograms.parseInterval(interval);
		String f = getField();
		Aggregation aggregation =
				Aggregation.of(a -> a.dateHistogram(dh -> dh.field(keyField).calendarInterval(calendarInterval))
						.aggregations(getId(), Aggregation.of(sa -> sa.stats(s -> s.field(f)))));
		addAggregation(getId(), aggregation, builder);
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		Aggregate agg = getAggregate(response);
		List<DateHistogramBucket> buckets = agg.dateHistogram().buckets().array();
		Map<String, ObjectNode> counts = Collections.emptyMap();
		if (!buckets.isEmpty()) {
			counts = getMap(getInterval(buckets));
			for (DateHistogramBucket bucket : buckets) {
				if (bucket.docCount() > 0) {
					long bucketTime = bucket.key();
					String key = getLabel(toLocalDateTime(bucketTime));
					if (range == null || counts.containsKey(key)) {
						ObjectNode entryNode = MoreObjects.firstNonNull(counts.get(key), Nodes.newObject());
						entryNode.put("label", key);
						entryNode.put("time", bucketTime);
						entryNode.put("count", bucket.docCount());
						if (!keyField.equals(valueField) && bucket.docCount() > 0) {
							StatsAggregate stats =
									bucket.aggregations().get(getId()).stats();
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

	private LocalInterval getInterval(List<DateHistogramBucket> buckets) {
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
		return min <= max ? new LocalInterval(toLocalDateTime(min), toLocalDateTime(max)) : null;
	}

	private LocalDateTime toLocalDateTime(long time) {
		return new LocalDateTime(time, DateTimeZone.UTC);
	}

	private Map<String, ObjectNode> getMap(LocalInterval interval) {
		Map<String, ObjectNode> counts = Maps.newTreeMap();
		if (interval != null) {
			for (LocalDateTime time : LocalIntervals.expand(interval.getStart(), interval.getEnd(), this.interval)) {
				String label = getLabel(time);
				ObjectNode node = Nodes.newObject();
				node.put("label", label);
				node.put("time", time.toDateTime(DateTimeZone.UTC).getMillis());
				node.put("count", 0);
				counts.put(label, node);
			}
		}
		return counts;
	}

	private String getLabel(LocalDateTime time) {
		return LocalIntervals.toString(time, interval);
	}
}
