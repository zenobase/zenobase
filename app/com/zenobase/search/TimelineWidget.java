package com.zenobase.search;

import java.util.Collections;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.zenobase.common.Intervals;
import com.zenobase.common.Nodes;

public class TimelineWidget implements Widget {

	private final String id;
	private final String field;
	private final String interval;
	private final Interval range;
	private final DateTimeZone timezone;

	public TimelineWidget(String id, String field, String interval, String range, DateTimeZone timezone) {
		this.id = id;
		this.field = field;
		this.interval = interval;
		this.range = !Strings.isNullOrEmpty(range) ? Intervals.valueOf(range) : null;
		this.timezone = timezone;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.dateHistogramFacet(id)
			.field(field).interval(interval)
			.preZone(timezone.toString())
			.preZoneAdjustLargeInterval(true));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		DateHistogramFacet facet = response.facets().facet(DateHistogramFacet.class, id);
		Map<String, Long> counts = Collections.emptyMap();
		if (!facet.getEntries().isEmpty()) {
			counts = getMap(getInterval(facet.getEntries()));
			for (DateHistogramFacet.Entry entry : facet.getEntries()) {
				counts.put(getLabel(toDateTime(entry.getTime())), entry.getCount());
			}
		}
		return toJson(counts);
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

	private Map<String, Long> getMap(Interval interval) {
		Map<String, Long> counts = Maps.newTreeMap();
		if (interval != null) {
			for (DateTime time : Intervals.expand(interval.getStart(), interval.getEnd(), this.interval)) {
				counts.put(getLabel(time), 0L);
			}
		}
		return counts;
	}

	private String getLabel(DateTime time) {
		return Intervals.toString(time, interval);
	}

	private JsonNode toJson(Map<String, Long> counts) {
		ArrayNode result = Nodes.newArray();
		for (Map.Entry<String, Long> entry : counts.entrySet()) {
			ObjectNode entryNode = result.addObject();
			entryNode.put("label", entry.getKey());
			entryNode.put("count", entry.getValue());
		}
		return result;
	}

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				return new TimelineWidget(
					options.get("id"),
					options.get("field"),
					options.get("interval"),
					options.get("range"),
					options.get("timezone", DateTimeZone.class, DateTimeZone.UTC));
			}
		};
	}
}
