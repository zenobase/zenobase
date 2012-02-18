package widgets;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;

import common.Nodes;

public class CountWidget implements Widget {

	private final String id;
	private final String field;
	private final int limit;

	private CountWidget(String id, String field, int limit) {
		this.id = id;
		this.field = field;
		this.limit = limit;
	}

	public String getId() {
		return id;
	}

	@Override
	public void configure(SearchRequestBuilder request) {
		request.addFacet(FacetBuilders.termsFacet(id)
			.field(field).size(limit)); 
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		TermsFacet terms = response.facets().facet(TermsFacet.class, id);
		for (TermsFacet.Entry entry : terms.entries()) {
			ObjectNode entryNode = result.addObject();
			entryNode.put("label", entry.getTerm());
			entryNode.put("count", entry.getCount());
		}
		return result;
	}

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				return new CountWidget(
					options.get("id"),
					options.get("field"),
					options.get("limit", Integer.class, 10));
			}
		};
	}
}
