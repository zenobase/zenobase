package com.zenobase.search;

import java.math.BigDecimal;
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

public class MultiplotWidget extends Widget {

	public static final String TYPE = "multiplot";

	private final String keyField;
	private final String xField, yField;
	private final String interval;
	private final DateTimeZone timezone;
	private final Unit<?> xUnit, yUnit;
	private final Statistic statistic;

	public MultiplotWidget(String id, String keyField, String xField, Unit<?> xUnit, String yField, Unit<?> yUnit, String interval, DateTimeZone timezone, Statistic statistic) {
		super(id);
		this.keyField = keyField;
		this.xField = xField;
		this.xUnit = xUnit;
		this.yField = yField;
		this.yUnit = yUnit;
		this.interval = interval;
		this.timezone = timezone;
		this.statistic = statistic;
	}

	private String getXId() {
		return getId() + "-x";
	}

	private String getYId() {
		return getId() + "-y";
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		addFacet(builder, getXId(), xField, xUnit);
		addFacet(builder, getYId(), yField, yUnit);
	}

	private void addFacet(SearchSourceBuilder builder, String id, String field, Unit<?> unit) {
		builder.facet(FacetBuilders.dateHistogramFacet(id)
			.keyField(keyField).valueField(unit == Unit.ONE ? field : field + "." + MeasurementField.VALUE_SI.getName())
			.interval(interval)
			.preZone(timezone.toString())
			.preZoneAdjustLargeInterval(true));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		Map<String, ObjectNode> values = Maps.newLinkedHashMap();
		process(response, values, getXId(), xField, xUnit);
		process(response, values, getYId(), yField, yUnit);
		return toJson(values.values());
	}

	private void process(SearchResponse response, Map<String, ObjectNode> values, String id, String field, Unit<?> unit) {
		DateHistogramFacet facet = response.facets().facet(DateHistogramFacet.class, id);
		for (DateHistogramFacet.Entry entry : facet.getEntries()) {
			if (entry.getTotalCount() > 0) {
				String key = getLabel(toDateTime(entry.getTime()));
				ObjectNode entryNode = values.get(key);
				if (entryNode == null) {
					entryNode = Nodes.newObject();
					entryNode.put("label", key);
					entryNode.put("time", entry.getTime() + timezone.getOffset(entry.getTime()));
					values.put(key, entryNode);
				}
				entryNode.put(field, getValue(statistic.getValue(entry), unit));
			}
		}
	}

	private static BigDecimal getValue(double value, Unit<?> unit) {
		return unit == Unit.ONE ? Measures.round(value) : Measures.convert(value, unit);
	}

	private JsonNode toJson(Iterable<ObjectNode> values) {
		ArrayNode node = Nodes.newArray();
		for (ObjectNode value : values) {
			node.add(value);
		}
		return node;
	}

	private DateTime toDateTime(long time) {
		return new DateTime(time, timezone);
	}

	private String getLabel(DateTime time) {
		return Intervals.toString(time, interval);
	}

	enum Statistic {
		AVG {
			@Override
			double getValue(DateHistogramFacet.Entry entry) {
				return entry.getMean();
			}
		},
		MIN {
			@Override
			double getValue(DateHistogramFacet.Entry entry) {
				return entry.getMin();
			}
		},
		MAX {
			@Override
			double getValue(DateHistogramFacet.Entry entry) {
				return entry.getMax();
			}
		},
		SUM {
			@Override
			double getValue(DateHistogramFacet.Entry entry) {
				return entry.getTotal();
			}
		},
		COUNT {
			@Override
			double getValue(DateHistogramFacet.Entry entry) {
				return entry.getCount();
			}
		};

		abstract double getValue(DateHistogramFacet.Entry entry);
	}

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				String xUnit = options.get("unit_x");
				String yUnit = options.get("unit_y");
				String statistic = options.get("statistic", String.class, "avg");
				return new MultiplotWidget(
					options.get("id"),
					Event.TIMESTAMP.getName(),
					options.get("field_x"),
					xUnit != null ? Measures.parseUnit(xUnit) : Unit.ONE,
					options.get("field_y"),
					yUnit != null ? Measures.parseUnit(yUnit) : Unit.ONE,
					options.get("interval", String.class, "month"),
					options.get("timezone", DateTimeZone.class, DateTimeZone.UTC),
					Statistic.valueOf(statistic.toUpperCase()));
			}
		};
	}
}
