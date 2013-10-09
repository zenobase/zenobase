package com.zenobase.search;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.histogram.HistogramFacet.ComparatorType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.json.MeasurementField;
import com.zenobase.json.Nodes;

public class HistogramFacet extends Facet {

	public static final String TYPE = "histogram";

	private final String field;
	private final long interval;
	private final Unit<?> unit;
	private final FilterBuilder filter;

	public HistogramFacet(String id, String field, long interval, Unit<?> unit, FilterBuilder filter) {
		super(id);
		this.field = field;
		this.interval = interval;
		this.unit = unit;
		this.filter = filter;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.histogramFacet(getId())
			.field(unit == Unit.ONE ? field : field + "." + MeasurementField.VALUE_SI.getName())
			.interval(getStandardInterval())
			.comparator(ComparatorType.KEY)
			.facetFilter(filter));
	}

	private long getStandardInterval() {
		if (unit == null || Measures.isStandard(unit)) {
			return interval;
		}
		if (SI.CELSIUS.equals(unit)) {
			return interval;
		}
		if (NonSI.FAHRENHEIT.equals(unit)) {
			return (long) (interval * 0.556);
		}
		return (long) unit.toStandardUnit().convert(interval);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		org.elasticsearch.search.facet.histogram.HistogramFacet facet = response.getFacets().facet(org.elasticsearch.search.facet.histogram.HistogramFacet.class, getId());
		for (org.elasticsearch.search.facet.histogram.HistogramFacet.Entry entry : Lists.reverse(facet.getEntries())) {
			ObjectNode entryNode = result.addObject();
			entryNode.put("count", entry.getCount());
			addValue(entryNode, "from", entry.getKey());
			addValue(entryNode, "to", entry.getKey() + getStandardInterval());
		}
		return result;
	}

	private void addValue(ObjectNode parent, String property, long value) {
		if (unit != Unit.ONE) {
			ObjectNode node = parent.putObject(property);
			node.put("@value", Measures.convert(value, unit));
			node.put("unit", unit.toString());
		} else {
			parent.put(property, Measures.round(value));
		}
	}

	public static FacetBuilder builder(final FilterParser filterParser) {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				String unit = options.get("unit");
				return new HistogramFacet(
					options.get("id"),
					options.get("field"),
					options.get("interval", Long.class, 10L),
					unit != null ? Measures.parseUnit(unit) : Unit.ONE,
					filterParser.parse(options.get("filter")));
			}
		};
	}
}
