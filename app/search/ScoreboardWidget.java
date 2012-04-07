package search;

import javax.measure.unit.Unit;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet.ComparatorType;

import schema.MeasurementField;

import common.Nodes;

public class ScoreboardWidget implements Widget {

	private final String id;
	private final String termField;
	private final String valueField;
	private final Unit<?> unit;
	private final ComparatorType order;
	private final int limit;

	private ScoreboardWidget(String id, String termField, String valueField, Unit<?> unit, ComparatorType order, int limit) {
		this.id = id;
		this.termField = termField; // TODO must be token
		this.valueField = valueField; // TODO must be measure
		this.unit = unit;
		this.order = order;
		this.limit = limit;
	}

	public String getId() {
		return id;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.termsStatsFacet(id)
			.keyField(termField).valueField(valueField + "." + MeasurementField.VALUE_SI.getName()).order(order).size(limit)); 
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		TermsStatsFacet terms = response.facets().facet(TermsStatsFacet.class, id);
		for (TermsStatsFacet.Entry entry : terms.entries()) {
			if (entry.getTotalCount() > 0) {
				ObjectNode entryNode = result.addObject();
				entryNode.put("label", entry.getTerm());
				entryNode.put("count", entry.getTotalCount());
				addValue(entryNode, "min",  entry.getMin());
				addValue(entryNode, "max", entry.getMax());
				addValue(entryNode, "sum", entry.getTotal());
				addValue(entryNode, "avg", entry.getMean());
			}
		}
		return result;
	}

	private void addValue(ObjectNode parent, String property, double value) {
		ObjectNode object = parent.putObject(property);
		object.put("@value", unit.getStandardUnit().getConverterTo(unit).convert(value));
		object.put("unit", unit.toString());
	}

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				return new ScoreboardWidget(
					options.get("id"),
					options.get("termField"),
					options.get("valueField"),
					Unit.valueOf(options.get("unit")),
					ComparatorType.valueOf(options.get("order", String.class, "term").toUpperCase()),
					options.get("limit", Integer.class, 10));
			}
		};
	}
}
