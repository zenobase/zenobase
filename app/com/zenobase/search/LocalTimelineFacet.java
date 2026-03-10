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
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import com.zenobase.common.LocalInterval;
import com.zenobase.common.LocalIntervals;
import com.zenobase.json.Nodes;

public class LocalTimelineFacet extends TimelineFacetSupport {

	private final String interval;
	private final LocalInterval range;

	public LocalTimelineFacet(String id, String keyField, String valueField, String interval, String range, Unit<?> unit, QueryBuilder filter) {
		super(id, keyField, valueField, unit, filter);
		this.interval = interval;
		this.range = !Strings.isNullOrEmpty(range) ? LocalIntervals.valueOf(range) : null;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		DateHistogramInterval histogramInterval = DateHistograms.parseInterval(interval);
		AggregationBuilder aggregation = AggregationBuilders.dateHistogram(getId())
			.field(keyField)
			.dateHistogramInterval(histogramInterval)
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
					String key = getLabel(toLocalDateTime(DateHistograms.toEpochMillis(bucket.getKey())));
					if (range == null || counts.containsKey(key)) {
						ObjectNode entryNode = Objects.firstNonNull(counts.get(key), Nodes.newObject());
						entryNode.put("label", key);
						entryNode.put("time", DateHistograms.toEpochMillis(bucket.getKey()));
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

	private LocalInterval getInterval(Iterable<? extends Histogram.Bucket> buckets) {
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
