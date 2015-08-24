package com.zenobase.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.range.RangeBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Rating;

public class RatingsFacet extends FilteredFacet {

	public static final String TYPE = "ratings";

	private final String field;
	private final double from, to, step;

	public RatingsFacet(String id, String field, int scale, FilterBuilder filter) {
		super(id, filter);
		this.field = field;
		step = Rating.MAX_VALUE / scale;
		from = step / 2;
		to = Rating.MAX_VALUE - from;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		RangeBuilder range = AggregationBuilders.range(getId()).field(field);
		range.addUnboundedTo(from);
		for (double i = from; i < to; i += step) {
			range.addRange(i, Math.min(i + step, to));
		}
		range.addUnboundedFrom(to);
		addAggregation(range, builder);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		Range range = getAggregation(response);
		for (Range.Bucket entry : ImmutableList.copyOf(range.getBuckets()).reverse()) {
			if (entry.getDocCount() > 0L) {
				ObjectNode entryNode = result.addObject();
				if (entry.getFrom().intValue() != Integer.MIN_VALUE) {
					entryNode.put("from", entry.getFrom().intValue());
				}
				if (entry.getTo().intValue() != Integer.MAX_VALUE) {
					entryNode.put("to", entry.getTo().intValue());
				}
				entryNode.put("count", entry.getDocCount());
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
