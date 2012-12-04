package com.zenobase.search;

import javax.measure.unit.Unit;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.histogram.HistogramFacet;
import org.elasticsearch.search.facet.histogram.HistogramFacet.ComparatorType;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.json.MeasurementField;
import com.zenobase.json.Nodes;

public class HistogramWidget extends Widget {

	public static final String TYPE = "histogram";

	private final String field;
	private final long interval;
	private final Unit<?> unit;

	public HistogramWidget(String id, String field, long interval, Unit<?> unit) {
		super(id);
		this.field = field;
		this.interval = interval;
		this.unit = unit;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.histogramFacet(getId())
			.field(unit == Unit.ONE ? field : field + "." + MeasurementField.VALUE_SI.getName())
			.interval(getStandardInterval())
			.comparator(ComparatorType.KEY));
	}

	private long getStandardInterval() {
		if (unit == null || unit.isStandardUnit()) {
			return interval;
		}
		return (long) unit.toStandardUnit().convert(interval);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		HistogramFacet facet = response.facets().facet(HistogramFacet.class, getId());
		for (HistogramFacet.Entry entry : Lists.reverse(facet.entries())) {
			ObjectNode entryNode = result.addObject();
			entryNode.put("count", entry.getCount());
			addValue(entryNode, "value", entry.getKey());
		}
		return result;
	}

	private void addValue(ObjectNode parent, String property, long value) {
		if (unit != Unit.ONE) {
			ObjectNode node = parent.putObject(property);
			node.put("@value", Measures.convert(value, unit));
			node.put("unit", unit.toString());
		} else {
			parent.put(property, value);
		}
	}

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				String unit = options.get("unit");
				return new HistogramWidget(
					options.get("id"),
					options.get("field"),
					options.get("interval", Long.class, 10L),
					unit != null ? Measures.valueOf(unit) : Unit.ONE);
			}
		};
	}
}
