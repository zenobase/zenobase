package search;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet.ComparatorType;

import common.Nodes;

public class ScoreboardWidget implements Widget {

	private final String id;
	private final String tokenField;
	private final String valueField;
	private final ComparatorType order;
	private final int limit;

	private ScoreboardWidget(String id, String tokenField, String valueField, ComparatorType order, int limit) {
		this.id = id;
		this.tokenField = tokenField;
		this.valueField = valueField;
		this.order = order;
		this.limit = limit;
	}

	public String getId() {
		return id;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.termsStatsFacet(id)
			.keyField(tokenField).valueField(valueField).order(order).size(limit)); 
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		TermsStatsFacet terms = response.facets().facet(TermsStatsFacet.class, id);
		for (TermsStatsFacet.Entry entry : terms.entries()) {
			if (!Double.isNaN(entry.getMin())) {
				ObjectNode entryNode = result.addObject();
				entryNode.put("label", entry.getTerm());
				entryNode.put("count", entry.getTotalCount());
				entryNode.put("min", entry.getMin());
				entryNode.put("max", entry.getMax());
				entryNode.put("sum", entry.getTotal());
				entryNode.put("avg", entry.getMean());
			}
		}
		return result;
	}

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				return new ScoreboardWidget(
					options.get("id"),
					options.get("tokenField"),
					options.get("valueField"),
					ComparatorType.valueOf(options.get("order", String.class, "term").toUpperCase()),
					options.get("limit", Integer.class, 10));
			}
		};
	}
}
