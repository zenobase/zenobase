package com.zenobase.search;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.range.RangeFacet;
import org.elasticsearch.search.facet.range.RangeFacetBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Rating;

public class RatingsFacet extends Facet {

	public static final String TYPE = "ratings";

	private final String field;
	private final double from, to, step;
	private final FilterBuilder filter;

	public RatingsFacet(String id, String field, int scale, FilterBuilder filter) {
		super(id);
		this.field = field;
		step = Rating.MAX_VALUE / scale;
		from = step / 2;
		to = Rating.MAX_VALUE - from;
		this.filter = filter;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		RangeFacetBuilder facet = FacetBuilders.rangeFacet(getId()).field(field);
		facet.addUnboundedFrom(from);
		for (double i = from; i < to; i += step) {
			facet.addRange(i, Math.min(i + step, to));
		}
		facet.addUnboundedTo(to);
		facet.facetFilter(filter);
		builder.facet(facet);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		RangeFacet facet = response.getFacets().facet(RangeFacet.class, getId());
		for (RangeFacet.Entry entry : Lists.reverse(facet.getEntries())) {
			if (entry.getCount() > 0L) {
				ObjectNode entryNode = result.addObject();
				if (!Double.isInfinite(entry.getFrom())) {
					entryNode.put("from", (int) entry.getFrom());
				}
				if (!Double.isInfinite(entry.getTo())) {
					entryNode.put("to", (int) entry.getTo());
				}
				entryNode.put("count", entry.getCount());
			}
		}
		return result;
	}

	public static FacetBuilder builder(final FilterParser filterParser) {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				return new RatingsFacet(
					options.get("id"),
					Event.RATING.getName(),
					options.get("scale", Integer.class, 5),
					filterParser.parse(options.get("filter")));
			}
		};
	}
}
