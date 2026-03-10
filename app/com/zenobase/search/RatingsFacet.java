package com.zenobase.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.RangeBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Rating;

public class RatingsFacet extends FilteredFacet {

	public static final String TYPE = "ratings";

	private final String field;
	private final double from, to, step;

	public RatingsFacet(String id, String field, int scale, Query filter) {
		super(id, filter);
		this.field = field;
		step = Rating.MAX_VALUE / scale;
		from = step / 2;
		to = Rating.MAX_VALUE - from;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		Aggregation range = Aggregation.of(a -> a.range(r -> {
			r.field(field);
			r.ranges(rng -> rng.to(String.valueOf(from)));
			for (double i = from; i < to; i += step) {
				double rangeFrom = i;
				double rangeTo = Math.min(i + step, to);
				r.ranges(rng -> rng.from(String.valueOf(rangeFrom)).to(String.valueOf(rangeTo)));
			}
			r.ranges(rng -> rng.from(String.valueOf(to)));
			return r;
		}));
		addAggregation(getId(), range, builder);
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		ArrayNode result = Nodes.newArray();
		Aggregate agg = getAggregate(response);
		for (RangeBucket entry : ImmutableList.copyOf(agg.range().buckets().array()).reverse()) {
			if (entry.docCount() > 0L) {
				ObjectNode entryNode = result.addObject();
				Double fromValue = entry.from();
				Double toValue = entry.to();
				if (fromValue != null && !Double.isInfinite(fromValue)) {
					entryNode.put("from", fromValue.intValue());
				}
				if (toValue != null && !Double.isInfinite(toValue)) {
					entryNode.put("to", toValue.intValue());
				}
				entryNode.put("count", entry.docCount());
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
