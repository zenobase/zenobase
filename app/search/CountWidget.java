package search;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;

import common.Nodes;

public class CountWidget implements Widget {

	private final String id;
	private final String field;
	private final int offset;
	private final int limit;

	private CountWidget(String id, String field, int offset, int limit) {
		this.id = id;
		this.field = field;
		this.offset = offset;
		this.limit = limit;
	}

	public String getId() {
		return id;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.termsFacet(id)
			.field(field).size(offset + limit)); 
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		TermsFacet terms = response.facets().facet(TermsFacet.class, id);
		for (TermsFacet.Entry entry : terms.entries().subList(offset, Math.min(terms.entries().size(), offset + limit))) {
			ObjectNode entryNode = result.addObject();
			entryNode.put("label", entry.getTerm());
			entryNode.put("count", entry.getCount());
		}
		if (terms.getOtherCount() > 0) {
			ObjectNode entryNode = result.addObject();
			entryNode.put("label", "...");
			entryNode.put("count", terms.getOtherCount());
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
					options.get("offset", Integer.class, 0),
					options.get("limit", Integer.class, 10));
			}
		};
	}
}
