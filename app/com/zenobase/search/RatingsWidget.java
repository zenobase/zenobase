package com.zenobase.search;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.range.RangeFacet;
import org.elasticsearch.search.facet.range.RangeFacetBuilder;
import com.google.common.collect.Lists;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Rating;

public class RatingsWidget extends Widget {

	public static final String TYPE = "ratings";

	private final String field;
	private final double from, to, step;

	public RatingsWidget(String id, String field, int scale) {
		super(id);
		this.field = field;
		step = Rating.MAX_VALUE / scale;
		from = step / 2;
		to = Rating.MAX_VALUE - from;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		RangeFacetBuilder facet = FacetBuilders.rangeFacet(getId()).field(field);
		facet.addUnboundedFrom(from);
		for (double i = from; i < to; i += step) {
			facet.addRange(i, Math.min(i + step, to));
		}
		facet.addUnboundedTo(to);
		builder.facet(facet);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		RangeFacet ratings = response.facets().facet(RangeFacet.class, getId());
		for (RangeFacet.Entry entry : Lists.reverse(ratings.entries())) {
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

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				return new RatingsWidget(
					options.get("id"),
					Event.RATING.getName(),
					options.get("scale", Integer.class, 5));
			}
		};
	}
}
