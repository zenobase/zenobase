package com.zenobase.search;

import java.util.Collections;
import java.util.Map;

import javax.measure.unit.Unit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import com.zenobase.common.OffsetIntervals;
import com.zenobase.json.Field;
import com.zenobase.json.MeasurementField;
import com.zenobase.json.Nodes;

public class OffsetTimelineFacet extends TimelineFacetSupport {

	private final String interval;
	private final Interval range;
	private final DateTimeZone timezone;

	public OffsetTimelineFacet(String id, String keyField, String valueField, String interval, String range, DateTimeZone timezone, Unit<?> unit, FilterBuilder filter) {
		super(id, keyField, valueField, unit, filter);
		this.interval = interval;
		this.range = !Strings.isNullOrEmpty(range) ? OffsetIntervals.valueOf(range) : null;
		this.timezone = timezone;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.dateHistogramFacet(getId())
			.keyField(keyField)
			.valueField(unit == Unit.ONE ? valueField : Field.concat(valueField, MeasurementField.VALUE_SI.getName()))
			.interval(interval)
			.preZone(timezone.toString())
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
				String key = getLabel(toDateTime(entry.getTime()));
				if (range == null || counts.containsKey(key)) {
					ObjectNode entryNode = Objects.firstNonNull(counts.get(key), Nodes.newObject());
					entryNode.put("label", key);
					entryNode.put("time", addOffset(entry.getTime()));
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

	private long addOffset(long time) {
		return time + (timezone != null ? timezone.getOffset(time) : 0);
	}

	private Interval getInterval(Iterable<? extends DateHistogramFacet.Entry> entries) {
		if (range != null) {
			return range;
		}
		long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
		for (DateHistogramFacet.Entry entry : entries) {
			min = Math.min(min, entry.getTime());
			max = Math.max(max, entry.getTime());
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
