package com.zenobase.search;

import java.util.Collections;
import java.util.Map;

import javax.measure.unit.Unit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import com.zenobase.common.LocalInterval;
import com.zenobase.common.LocalIntervals;
import com.zenobase.json.Field;
import com.zenobase.json.LocalDateTimeField;
import com.zenobase.json.MeasurementField;
import com.zenobase.json.Nodes;

public class LocalTimelineFacet extends TimelineFacetSupport {

	private final String interval;
	private final LocalInterval range;

	public LocalTimelineFacet(String id, String keyField, String valueField, String interval, String range, Unit<?> unit, FilterBuilder filter) {
		super(id, keyField, valueField, unit, filter);
		this.interval = interval;
		this.range = !Strings.isNullOrEmpty(range) ? LocalIntervals.valueOf(range) : null;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.dateHistogramFacet(getId())
			.keyField(LocalDateTimeField.getLocalTimePath(keyField))
			.valueField(unit == Unit.ONE ? valueField : Field.concat(valueField, MeasurementField.VALUE_SI.getName()))
			.interval(interval)
			.preZone(DateTimeZone.UTC.toString())
			.preZoneAdjustLargeInterval(true)
			.facetFilter(filter));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		DateHistogramFacet facet = response.getFacets().facet(DateHistogramFacet.class, getId());
		Map<String, ObjectNode> counts = Collections.emptyMap();
		if (!facet.getEntries().isEmpty()) {
			counts = getMap(getInterval(facet.getEntries()));
			for (DateHistogramFacet.Entry entry : facet.getEntries()) {
				String key = getLabel(toLocalDateTime(entry.getTime()));
				if (range == null || counts.containsKey(key)) {
					ObjectNode entryNode = Objects.firstNonNull(counts.get(key), Nodes.newObject());
					entryNode.put("label", key);
					entryNode.put("time", entry.getTime());
					entryNode.put("count", entry.getTotalCount());
					if (!keyField.equals(valueField) && entry.getTotalCount() > 0) {
						addValue(entryNode, "min",  entry.getMin());
						addValue(entryNode, "max", entry.getMax());
						addValue(entryNode, "sum", entry.getTotal());
						addValue(entryNode, "avg", entry.getMean());
					}
					counts.put(key, entryNode);

				}
			}
		}
		return toJson(counts.values());
	}

	private LocalInterval getInterval(Iterable<? extends DateHistogramFacet.Entry> entries) {
		if (range != null) {
			return range;
		}
		long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
		for (DateHistogramFacet.Entry entry : entries) {
			min = Math.min(min, entry.getTime());
			max = Math.max(max, entry.getTime());
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
