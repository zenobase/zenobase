package com.zenobase.search;

import java.util.Collections;
import java.util.Map;

import javax.measure.unit.Unit;

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
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import com.zenobase.common.Intervals;
import com.zenobase.common.Measures;
import com.zenobase.json.MeasurementField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class TimelineWidget extends Widget {

	public static final String TYPE = "timeline";

	private final String keyField;
	private final String valueField;
	private final String interval;
	private final Interval range;
	private final DateTimeZone timezone;
	private final Unit<?> unit;

	public TimelineWidget(String id, String keyField, String valueField, String interval, String range, DateTimeZone timezone, Unit<?> unit) {
		super(id);
		this.keyField = keyField;
		this.valueField = valueField;
		this.interval = interval;
		this.range = !Strings.isNullOrEmpty(range) ? Intervals.valueOf(range) : null;
		this.timezone = timezone;
		this.unit = unit;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.dateHistogramFacet(getId())
			.keyField(keyField).valueField(unit == Unit.ONE ? valueField : valueField + "." + MeasurementField.VALUE_SI.getName())
			.interval(interval)
			.preZone(timezone.toString().replaceAll("\\+", ""))  // strip '+' as workaround for https://github.com/elasticsearch/elasticsearch/issues/2141
			.preZoneAdjustLargeInterval(true));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		DateHistogramFacet facet = response.facets().facet(DateHistogramFacet.class, getId());
		Map<String, ObjectNode> counts = Collections.emptyMap();
		if (!facet.getEntries().isEmpty()) {
			counts = getMap(getInterval(facet.getEntries()));
			for (DateHistogramFacet.Entry entry : facet.getEntries()) {
				String key = getLabel(toDateTime(entry.getTime()));
				if (range == null || counts.containsKey(key)) {
					ObjectNode entryNode = Objects.firstNonNull(counts.get(key), Nodes.newObject());
					entryNode.put("label", key);
					entryNode.put("count", entry.getTotalCount());
					if (unit != Unit.ONE && entry.getTotalCount() > 0) {
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

	private JsonNode toJson(Iterable<ObjectNode> values) {
		ArrayNode node = Nodes.newArray();
		for (ObjectNode value : values) {
			node.add(value);
		}
		return node;
	}

	private void addValue(ObjectNode parent, String property, double value) {
		ObjectNode node = parent.putObject(property);
		node.put("@value", Measures.convert(value, unit));
		node.put("unit", unit.toString());
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
			for (DateTime time : Intervals.expand(interval.getStart(), interval.getEnd(), this.interval)) {
				String label = getLabel(time);
				ObjectNode node = Nodes.newObject();
				node.put("label", label);
				node.put("count", 0);
				counts.put(label, node);
			}
		}
		return counts;
	}

	private String getLabel(DateTime time) {
		return Intervals.toString(time, interval);
	}

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				String unit = options.get("unit");
				return new TimelineWidget(
					options.get("id"),
					options.get("keyField", String.class, Event.TIMESTAMP.getName()),
					options.get("valueField", String.class, Event.TIMESTAMP.getName()),
					options.get("interval", String.class, "month"),
					options.get("range"),
					options.get("timezone", DateTimeZone.class, DateTimeZone.UTC),
					unit != null ? Measures.valueOf(unit) : Unit.ONE);
			}
		};
	}
}
