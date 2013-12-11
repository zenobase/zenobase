package com.zenobase.search;

import javax.measure.unit.Unit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.statistical.StatisticalFacet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Measures;
import com.zenobase.json.Field;
import com.zenobase.json.MeasurementField;
import com.zenobase.json.Nodes;

public class StatsFacet extends Facet {

	public static final String TYPE = "stats";

	private final String field;
	private final Unit<?> unit;
	private final FilterBuilder filter;

	public StatsFacet(String id, String field, Unit<?> unit, FilterBuilder filter) {
		super(id);
		this.field = field;
		this.unit = unit;
		this.filter = filter;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.statisticalFacet(getId())
			.field(unit == Unit.ONE ? field : Field.concat(field, MeasurementField.VALUE_SI.getName()))
			.facetFilter(filter));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		StatisticalFacet facet = response.getFacets().facet(StatisticalFacet.class, getId());
		ObjectNode node = Nodes.newObject();
		node.put("count", facet.getCount());
		if (facet.getCount() > 0) {
			put(node, "min",  facet.getMin());
			put(node, "max",  facet.getMax());
			put(node, "sum",  facet.getTotal());
			put(node, "avg",  facet.getMean());
			put(node, "stdev", facet.getStdDeviation());
		}
		return node;
	}

	private void put(ObjectNode parent, String property, double value) {
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
				return new StatsFacet(
					options.get("id"),
					options.get("field"),
					getUnit(options.get("unit")),
					filterParser.parse(options.get("filter")));
			}

			private Unit<?> getUnit(String value) {
				return value != null ? Measures.parseUnit(value) : Unit.ONE;
			}
		};
	}
}
