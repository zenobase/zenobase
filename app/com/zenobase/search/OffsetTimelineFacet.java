package com.zenobase.search;

import java.util.Collections;
import java.util.Map;

import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.opensearch.search.aggregations.bucket.histogram.Histogram;
import org.opensearch.search.aggregations.metrics.Stats;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import com.zenobase.common.OffsetIntervals;
import com.zenobase.json.Nodes;

public class OffsetTimelineFacet extends TimelineFacetSupport {

	private final String interval;
	private final Interval range;
	private final DateTimeZone timezone;

	public OffsetTimelineFacet(String id, String keyField, String valueField, String interval, String range, DateTimeZone timezone, Unit<?> unit, QueryBuilder filter) {
		super(id, keyField, valueField, unit, filter);
		this.interval = interval;
		this.range = !Strings.isNullOrEmpty(range) ? OffsetIntervals.valueOf(range) : null;
		this.timezone = timezone;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		DateHistogramInterval histogramInterval = DateHistograms.parseInterval(interval);
		AggregationBuilder aggregation = AggregationBuilders.dateHistogram(getId())
			.field(keyField)
			.dateHistogramInterval(histogramInterval)
			.timeZone(timezone.toTimeZone().toZoneId())
			.subAggregation(AggregationBuilders.stats(getId()).field(getField()));
		addAggregation(aggregation, builder);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		Histogram histogram = getAggregation(response);
		Map<String, ObjectNode> counts = Collections.emptyMap();
		if (!histogram.getBuckets().isEmpty()) {
			counts = getMap(getInterval(histogram.getBuckets()));
			for (Histogram.Bucket bucket : histogram.getBuckets()) {
				if (bucket.getDocCount() > 0) {
					String key = getLabel(toDateTime(DateHistograms.toEpochMillis(bucket.getKey())));
					if (range == null || counts.containsKey(key)) {
						ObjectNode entryNode = Objects.firstNonNull(counts.get(key), Nodes.newObject());
						entryNode.put("label", key);
						entryNode.put("time", addOffset(DateHistograms.toEpochMillis(bucket.getKey())));
						entryNode.put("count", bucket.getDocCount());
						if (!keyField.equals(valueField) && bucket.getDocCount() > 0) {
							Stats stats = bucket.getAggregations().get(getId());
							addValue(entryNode, "min",  stats.getMin());
							addValue(entryNode, "max", stats.getMax());
							addValue(entryNode, "sum", stats.getSum());
							addValue(entryNode, "avg", stats.getAvg());
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

	private Interval getInterval(Iterable<? extends Histogram.Bucket> buckets) {
		if (range != null) {
			return range;
		}
		long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
		for (Histogram.Bucket bucket : buckets) {
			if (bucket.getDocCount() > 0) {
				min = Math.min(min, DateHistograms.toEpochMillis(bucket.getKey()));
				max = Math.max(max, DateHistograms.toEpochMillis(bucket.getKey()));
			}
		}
		return min <= max ? new Interval(toDateTime(min), toDateTime(max)) : null;
	}

	private DateTime toDateTime(long time) {
		return new DateTime(time, timezone);
	}

	private Map<String, ObjectNode> getMap(Interval interval) {
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
