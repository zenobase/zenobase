package com.zenobase.search;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.json.JsonData;
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

	public RatingsFacet(String id, String field, int scale, @Nullable Query filter) {
		super(id, filter);
		this.field = field;
		step = (double) Rating.MAX_VALUE / scale;
		from = step / 2;
		to = Rating.MAX_VALUE - from;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		Aggregation range = Aggregation.of(a -> a.range(r -> {
			r.field(field);
			r.ranges(rng -> rng.to(JsonData.of(from)));
			for (double i = from; i < to; i += step) {
				double rangeFrom = i;
				double rangeTo = Math.min(i + step, to);
				r.ranges(rng -> rng.from(JsonData.of(rangeFrom)).to(JsonData.of(rangeTo)));
			}
			r.ranges(rng -> rng.from(JsonData.of(to)));
			return r;
		}));
		addAggregation(getId(), range, builder);
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		ArrayNode result = Nodes.newArray();
		Aggregate aggregate = getAggregate(response);
		if (aggregate == null) {
			return result;
		}
		for (RangeBucket entry :
				ImmutableList.copyOf(aggregate.range().buckets().array()).reverse()) {
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
				Objects.requireNonNull(options.get("id")),
				Event.RATING.getName(),
				Objects.requireNonNull(options.get("scale", Integer.class, 5)),
				filterParser.parse(options.get("filter")));
	}
}
