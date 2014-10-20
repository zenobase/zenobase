package com.zenobase.search;

import javax.measure.unit.Unit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet.ComparatorType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.Field;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Nodes;

public class ScoreboardFacet extends Facet {

	public static final String TYPE = "scoreboard";

	private final String termField;
	private final String valueField;
	private final Unit<?> unit;
	private final ComparatorType order;
	private final int limit;
	private final FilterBuilder filter;

	private ScoreboardFacet(String id, String termField, String valueField, Unit<?> unit, ComparatorType order, int limit, FilterBuilder filter) {
		super(id);
		this.termField = termField;
		this.valueField = valueField;
		this.unit = unit;
		this.order = order;
		this.limit = limit;
		this.filter = filter;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.termsStatsFacet(getId())
			.keyField(termField).valueField(unit == Unit.ONE ? valueField : Field.concat(valueField, DecimalMeasureField.VALUE_SI.getName()))
			.order(order).size(limit)
			.facetFilter(filter));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		TermsStatsFacet terms = response.getFacets().facet(TermsStatsFacet.class, getId());
		for (TermsStatsFacet.Entry entry : terms.getEntries()) {
			if (entry.getTotalCount() > 0) {
				ObjectNode entryNode = result.addObject();
				entryNode.put("label", entry.getTerm().toString());
				entryNode.put("count", entry.getTotalCount());
				addValue(entryNode, "min", entry.getMin());
				addValue(entryNode, "max", entry.getMax());
				addValue(entryNode, "sum", entry.getTotal());
				addValue(entryNode, "avg", entry.getMean());
			}
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
				return new ScoreboardFacet(
					options.get("id"),
					options.get("key_field"),
					options.get("value_field"),
					unit != null ? Units.valueOf(unit) : Unit.ONE,
					ComparatorType.valueOf(options.get("order", String.class, "term").toUpperCase()),
					options.get("limit", Integer.class, 10),
					filterParser.parse(options.get("filter")));
			}
		};
	}
}
