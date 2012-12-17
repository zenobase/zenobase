package com.zenobase.search;

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
import com.google.common.collect.Maps;

import com.zenobase.common.Intervals;
import com.zenobase.common.Measures;
import com.zenobase.json.MeasurementField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class PlotWidget extends Widget {

	public static final String TYPE = "plot";

	private final String keyField;
	private final String valueField;
	private final String interval;
	private final DateTimeZone timezone;
	private final Unit<?> unit;

	public PlotWidget(String id, String keyField, String valueField, String interval, DateTimeZone timezone, Unit<?> unit) {
		super(id);
		this.keyField = keyField;
		this.valueField = valueField;
		this.interval = interval;
		this.timezone = timezone;
		this.unit = unit;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.dateHistogramFacet(getId())
			.keyField(keyField).valueField(unit == Unit.ONE ? valueField : valueField + "." + MeasurementField.VALUE_SI.getName())
			.interval(interval)
			.preZone(timezone.toString())
			.preZoneAdjustLargeInterval(true));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		DateHistogramFacet facet = response.facets().facet(DateHistogramFacet.class, getId());
		Map<String, ObjectNode> values = Maps.newLinkedHashMap();
		if (!facet.getEntries().isEmpty()) {
			for (DateHistogramFacet.Entry entry : facet.getEntries()) {
				if (entry.getTotalCount() > 0) {
					ObjectNode entryNode = Nodes.newObject();
					String key = getLabel(toDateTime(entry.getTime()));
					entryNode.put("label", key);
					entryNode.put("time", entry.getTime());
					entryNode.put("count", entry.getTotalCount());
					if (!keyField.equals(valueField)) {
						addValue(entryNode, "min",  entry.getMin());
						addValue(entryNode, "max", entry.getMax());
						addValue(entryNode, "sum", entry.getTotal());
						addValue(entryNode, "avg", entry.getMean());
					}
					values.put(key, entryNode);
				}
			}
		}
		return toJson(values.values());
	}

	private JsonNode toJson(Iterable<ObjectNode> values) {
		ArrayNode node = Nodes.newArray();
		for (ObjectNode value : values) {
			node.add(value);
		}
		return node;
	}

	private void addValue(ObjectNode parent, String property, double value) {
		if (unit != Unit.ONE) {
			ObjectNode node = parent.putObject(property);
			node.put("@value", Measures.convert(value, unit));
			node.put("unit", unit.toString());
		} else {
			parent.put(property, Measures.round(value));
		}
	}

	private DateTime toDateTime(long time) {
		return new DateTime(time, timezone);
	}

	private String getLabel(DateTime time) {
		return Intervals.toString(time, interval);
	}

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				String unit = options.get("unit");
				return new PlotWidget(
					options.get("id"),
					Event.TIMESTAMP.getName(),
					options.get("field", String.class, Event.TIMESTAMP.getName()),
					options.get("interval", String.class, "month"),
					options.get("timezone", DateTimeZone.class, DateTimeZone.UTC),
					unit != null ? Measures.valueOf(unit) : Unit.ONE);
			}
		};
	}
}
