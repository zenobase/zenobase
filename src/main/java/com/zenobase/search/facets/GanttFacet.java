package com.zenobase.search.facets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.LongField;
import com.zenobase.json.Nodes;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;
import com.zenobase.search.constraints.FilterParser;
import java.util.Collections;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.StatsAggregate;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

public class GanttFacet extends FilteredFacet {

	public static final String TYPE = "gantt";

	private static final TokenField LABEL = new TokenField("label");
	private static final LongField COUNT = new LongField("count");
	private static final DateTimeField FIRST = new DateTimeField("first");
	private static final DateTimeField LAST = new DateTimeField("last");

	private final String keyField;
	private final String valueField;
	private final String order;
	private final int limit;
	private final DateTimeZone timezone;

	private GanttFacet(
		String id,
		String keyField,
		String valueField,
		String order,
		int limit,
		DateTimeZone timezone,
		@Nullable Query filter
	) {
		super(id, filter);
		this.keyField = keyField;
		this.valueField = valueField;
		this.order = order;
		this.limit = limit;
		this.timezone = timezone;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		Aggregation aggregation = Aggregation.of(a ->
			a
				.terms(t -> {
					t.field(keyField).size(limit);
					boolean asc = !order.startsWith("-");
					String orderField = asc ? order : order.substring(1);
					SortOrder sortOrder = asc ? SortOrder.Asc : SortOrder.Desc;
					switch (orderField) {
						case "count" -> t.order(Collections.singletonMap("_count", sortOrder));
						case "term" -> t.order(Collections.singletonMap("_key", sortOrder));
						case "min" -> t.order(Collections.singletonMap(getId() + ".min", sortOrder));
						case "max" -> t.order(Collections.singletonMap(getId() + ".max", sortOrder));
					}
					return t;
				})
				.aggregations(getId(), Aggregation.of(sa -> sa.stats(s -> s.field(valueField))))
		);
		addAggregation(getId(), aggregation, builder);
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		ArrayNode result = Nodes.newArray();
		Aggregate aggregate = getAggregate(response);
		if (aggregate == null) {
			return result;
		}
		for (TermsBuckets.Bucket bucket : TermsBuckets.buckets(aggregate)) {
			Aggregate bucketAggregate = bucket.aggregations().get(getId());
			if (bucketAggregate == null) {
				continue;
			}
			StatsAggregate stats = bucketAggregate.stats();
			DateTime first = asDateTime(stats.min());
			if (first != null) {
				ObjectNode entryNode = result.addObject();
				LABEL.setValue(entryNode, bucket.key());
				COUNT.setValue(entryNode, bucket.docCount());
				FIRST.setValue(entryNode, first);
				LAST.setValue(entryNode, asDateTime(stats.max()));
			}
		}
		return result;
	}

	private @Nullable DateTime asDateTime(@Nullable Double value) {
		return value != null && !Double.isInfinite(value) ? new DateTime(value.longValue(), timezone) : null;
	}

	public static FacetBuilder builder(FilterParser filterParser) {
		return options ->
			new GanttFacet(
				Objects.requireNonNull(options.get("id")),
				Objects.requireNonNull(options.get("field")),
				Objects.requireNonNull(options.get("key_field", String.class, Event.TIMESTAMP.getName())),
				Objects.requireNonNull(options.get("order", String.class, "-max")),
				Objects.requireNonNull(options.get("limit", Integer.class, 10)),
				Objects.requireNonNull(options.get("timezone", DateTimeZone.class, DateTimeZone.UTC)),
				filterParser.parse(options.get("filter"))
			);
	}
}
