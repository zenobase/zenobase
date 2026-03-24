package com.zenobase.search;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.StatsAggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.json.DateTimeField;
import com.zenobase.json.LongField;
import com.zenobase.json.Nodes;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;

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

	private GanttFacet(String id, String keyField, String valueField, String order, int limit, DateTimeZone timezone, Query filter) {
		super(id, filter);
		this.keyField = keyField;
		this.valueField = valueField;
		this.order = order;
		this.limit = limit;
		this.timezone = timezone;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		Aggregation aggregation = Aggregation.of(a -> a
			.terms(t -> {
				t.field(keyField).size(limit);
				boolean asc = !order.startsWith("-");
				String orderField = asc ? order : order.substring(1);
				SortOrder sortOrder = asc ? SortOrder.Asc : SortOrder.Desc;
				switch (orderField) {
					case "count":
						t.order(Collections.singletonMap("_count", sortOrder));
						break;
					case "term":
						t.order(Collections.singletonMap("_key", sortOrder));
						break;
					case "min":
						t.order(Collections.singletonMap(getId() + ".min", sortOrder));
						break;
					case "max":
						t.order(Collections.singletonMap(getId() + ".max", sortOrder));
						break;
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
		Aggregate agg = getAggregate(response);
		for (StringTermsBucket bucket : agg.sterms().buckets().array()) {
			StatsAggregate stats = bucket.aggregations().get(getId()).stats();
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

	private DateTime asDateTime(Double value) {
		return value != null && !Double.isInfinite(value) ? new DateTime(value.longValue(), timezone) : null;
	}

	public static FacetBuilder builder(FilterParser filterParser) {
		return options -> new GanttFacet(
			options.get("id"),
			options.get("field"),
			options.get("key_field", String.class, Event.TIMESTAMP.getName()),
			options.get("order", String.class, "-max"),
			options.get("limit", Integer.class, 10),
			options.get("timezone", DateTimeZone.class, DateTimeZone.UTC),
			filterParser.parse(options.get("filter")));
	}
}
