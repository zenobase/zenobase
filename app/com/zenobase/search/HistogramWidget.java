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

public class HistogramWidget implements Widget {

	public static final String TYPE = "histogram";

	private final String id;
	private final String field;
	private final double from, to, step;

	public HistogramWidget(String id, String field, double from, double to, double step) {
		this.id = id;
		this.field = field;
		this.from = from;
		this.to = to;
		this.step = step;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		RangeFacetBuilder facet = FacetBuilders.rangeFacet(id).field(field);
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
		RangeFacet ratings = response.facets().facet(RangeFacet.class, id);
		for (RangeFacet.Entry entry : Lists.reverse(ratings.entries())) {
			if (entry.getCount() > 0L) {
				ObjectNode entryNode = result.addObject();
				if (!Double.isInfinite(entry.getFrom())) {
					entryNode.put("from", entry.getFrom());
				}
				if (!Double.isInfinite(entry.getTo())) {
					entryNode.put("to", entry.getTo());
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
				return new HistogramWidget(
					options.get("id"),
					options.get("field"),
					options.get("from", Double.class, 10.0),
					options.get("to", Double.class, 90.0),
					options.get("step", Double.class, 20.0));
			}
		};
	}
}
