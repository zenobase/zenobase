package com.zenobase.search;

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType;

import com.zenobase.json.Nodes;

public class CountFacet extends Facet {

	public static final String TYPE = "count";
	public static final String LABEL_MORE = "...";

	private final String field;
	private final ComparatorType order;
	private final int offset;
	private final int limit;

	private CountFacet(String id, String field, String order, boolean reverse, int offset, int limit) {
		super(id);
		this.field = field;
		this.order = ComparatorType.fromString((reverse ? "reverse_" : "") + order);
		this.offset = offset;
		this.limit = limit;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.termsFacet(getId())
			.field(field).size(offset + limit).order(order));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		TermsFacet terms = response.getFacets().facet(TermsFacet.class, getId());
		List<? extends TermsFacet.Entry> entries = terms.getEntries();
		for (TermsFacet.Entry entry : entries.subList(offset, Math.min(entries.size(), offset + limit))) {
			ObjectNode entryNode = result.addObject();
			entryNode.put("label", entry.getTerm().toString());
			entryNode.put("count", entry.getCount());
		}
		if (terms.getOtherCount() > 0) {
			ObjectNode entryNode = result.addObject();
			entryNode.put("label", LABEL_MORE);
			entryNode.put("count", terms.getOtherCount());
		}
		return result;
	}

	public static FacetBuilder builder() {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				return new CountFacet(
					options.get("id"),
					options.get("field"),
					options.get("order", String.class, "count"),
					options.get("reverse", Boolean.class, Boolean.FALSE),
					options.get("offset", Integer.class, 0),
					options.get("limit", Integer.class, 10));
			}
		};
	}
}
