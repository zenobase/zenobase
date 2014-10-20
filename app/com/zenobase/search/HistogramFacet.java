package com.zenobase.search;

import javax.measure.unit.Unit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.histogram.HistogramFacet.ComparatorType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.Field;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Nodes;
import com.zenobase.search.facet.decimalhistogram.DecimalHistogramFacet;
import com.zenobase.search.facet.decimalhistogram.DecimalHistogramFacetBuilder;

public class HistogramFacet extends Facet {

	public static final String TYPE = "histogram";

	private final String field;
	private final double interval;
	private final Unit<?> unit;
	private final FilterBuilder filter;

	public HistogramFacet(String id, String field, double interval, Unit<?> unit, FilterBuilder filter) {
		super(id);
		this.field = field;
		this.interval = interval;
		this.unit = unit;
		this.filter = filter;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		String field = unit == Unit.ONE ? this.field : Field.concat(this.field, DecimalMeasureField.VALUE_SI.getName());
		builder.facet(new DecimalHistogramFacetBuilder(getId(), field, getStandardInterval(), getStandardOffset(), ComparatorType.KEY).facetFilter(filter));
	}

	private double getStandardInterval() {
		if (unit == null || Units.isStandard(unit) || Units.C.equals(unit)) {
			return interval;
		}
		if (Units.F.equals(unit)) {
			return interval * (5.0 / 9.0);
		}
		return unit.toStandardUnit().convert(interval);
	}

	private double getStandardOffset() {
		if (Units.C.equals(unit)) {
			return -273.15;
		}
		if (Units.F.equals(unit)) {
			return -459.67 * (5.0 / 9.0);
		}
		return 0.0;
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		DecimalHistogramFacet facet = response.getFacets().facet(DecimalHistogramFacet.class, getId());
		for (DecimalHistogramFacet.Entry entry : Lists.reverse(facet.getEntries())) {
			ObjectNode entryNode = result.addObject();
			entryNode.put("count", entry.getCount());
			double interval = getStandardInterval();
			double from = entry.getKey(interval) - getStandardOffset();
			addValue(entryNode, "from", from);
			addValue(entryNode, "to", from + interval);
		}
		return result;
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

	public static FacetBuilder builder(final FilterParser filterParser) {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				String unit = options.get("unit");
				return new HistogramFacet(
					options.get("id"),
					options.get("field"),
					options.get("interval", Double.class, 10.0),
					unit != null ? Units.valueOf(unit) : Unit.ONE,
					filterParser.parse(options.get("filter")));
			}
		};
	}
}
