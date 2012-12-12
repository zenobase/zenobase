package com.zenobase.search;

import javax.measure.unit.Unit;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet.ComparatorType;

import com.zenobase.common.Measures;
import com.zenobase.json.MeasurementField;
import com.zenobase.json.Nodes;

public class ScoreboardWidget extends Widget {

	public static final String TYPE = "scoreboard";

	private final String termField;
	private final String valueField;
	private final Unit<?> unit;
	private final ComparatorType order;
	private final int limit;

	private ScoreboardWidget(String id, String termField, String valueField, Unit<?> unit, ComparatorType order, int limit) {
		super(id);
		this.termField = termField;
		this.valueField = valueField;
		this.unit = unit;
		this.order = order;
		this.limit = limit;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.termsStatsFacet(getId())
			.keyField(termField).valueField(unit == Unit.ONE ? valueField : valueField + "." + MeasurementField.VALUE_SI.getName()).order(order).size(limit));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		TermsStatsFacet terms = response.facets().facet(TermsStatsFacet.class, getId());
		for (TermsStatsFacet.Entry entry : terms.entries()) {
			if (entry.getTotalCount() > 0) {
				ObjectNode entryNode = result.addObject();
				entryNode.put("label", entry.getTerm());
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

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				String unit = options.get("unit");
				return new ScoreboardWidget(
					options.get("id"),
					options.get("termField"),
					options.get("valueField"),
					unit != null ? Measures.valueOf(unit) : Unit.ONE,
					ComparatorType.valueOf(options.get("order", String.class, "term").toUpperCase()),
					options.get("limit", Integer.class, 10));
			}
		};
	}
}
