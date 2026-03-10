package com.zenobase.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.range.Range;
import org.opensearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Rating;

public class RatingsFacet extends FilteredFacet {

	public static final String TYPE = "ratings";

	private final String field;
	private final double from, to, step;

	public RatingsFacet(String id, String field, int scale, QueryBuilder filter) {
		super(id, filter);
		this.field = field;
		step = Rating.MAX_VALUE / scale;
		from = step / 2;
		to = Rating.MAX_VALUE - from;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		RangeAggregationBuilder range = AggregationBuilders.range(getId()).field(field);
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
				Number fromValue = (Number) entry.getFrom();
				Number toValue = (Number) entry.getTo();
				if (fromValue != null && fromValue.intValue() != Integer.MIN_VALUE && !Double.isInfinite(fromValue.doubleValue())) {
					entryNode.put("from", fromValue.intValue());
				}
				if (toValue != null && toValue.intValue() != Integer.MAX_VALUE && !Double.isInfinite(toValue.doubleValue())) {
					entryNode.put("to", toValue.intValue());
				}
				entryNode.put("count", entry.getDocCount());
			}
		}
		return result;
	}

	public static FacetBuilder builder(FilterParser filterParser) {
		return options -> new RatingsFacet(
			options.get("id"),
			Event.RATING.getName(),
			options.get("scale", Integer.class, 5),
			filterParser.parse(options.get("filter")));
	}
}
