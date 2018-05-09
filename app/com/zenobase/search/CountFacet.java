package com.zenobase.search;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.zenobase.json.Nodes;

public class CountFacet extends FilteredFacet {

	public static final String TYPE = "count";
	public static final String LABEL_MORE = "...";

	private final String field;
	private final Terms.Order order;
	private final int offset;
	private final int limit;

	private CountFacet(String id, String field, String order, int offset, int limit, FilterBuilder filter) {
		super(id, filter);
		this.field = field;
		this.order = parseOrder(order);
		this.offset = offset;
		this.limit = limit;
	}

	private Terms.Order parseOrder(String s) {
		boolean asc = !s.startsWith("-");
		if (!asc) {
			s = s.substring(1);
		}
		if ("count".equals(s)) {
			return Terms.Order.count(asc);
		} else if ("term".equals(s)) {
			return Terms.Order.term(asc);
		} else {
			throw new IllegalArgumentException("Invalid order: " + s);
		}
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		TermsBuilder terms = AggregationBuilders.terms(getId())
			.field(field).size(offset + limit).order(order);
		addAggregation(terms, builder);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		Terms terms = getAggregation(response);
		List<Terms.Bucket> entries = terms.getBuckets();
		if (offset < entries.size()) {
			for (Terms.Bucket entry : entries.subList(offset, Math.min(entries.size(), offset + limit))) {
				ObjectNode entryNode = result.addObject();
				entryNode.put("label", entry.getKey());
				entryNode.put("count", entry.getDocCount());
			}
			if (terms.getSumOfOtherDocCounts() > 0) {
				ObjectNode entryNode = result.addObject();
				entryNode.put("label", LABEL_MORE);
				entryNode.put("count", terms.getSumOfOtherDocCounts());
			}
		}
		return result;
	}

	public static FacetBuilder builder(FilterParser filterParser) {
		return options -> new CountFacet(
			options.get("id"),
			options.get("field"),
			options.get("order", String.class, "-count"),
			options.get("offset", Integer.class, 0),
			options.get("limit", Integer.class, 10),
			filterParser.parse(options.get("filter")));
	}
}
