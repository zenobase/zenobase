package com.zenobase.search;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
	private final Terms.Order order;
	private final int limit;
	private final DateTimeZone timezone;

	private GanttFacet(String id, String keyField, String valueField, String order, int limit, DateTimeZone timezone, FilterBuilder filter) {
		super(id, filter);
		this.keyField = keyField;
		this.valueField = valueField;
		this.order = parseOrder(order, false);
		this.limit = limit;
		this.timezone = timezone;
	}

	private Terms.Order parseOrder(String s, boolean reverse) {
		if ("count".equals(s)) {
			return Terms.Order.count(reverse);
		} else if ("term".equals(s)) {
			return Terms.Order.term(!reverse);
		} else if ("min".equals(s)) {
			return Terms.Order.aggregation(getId(), "min", !reverse);
		} else if ("max".equals(s)) {
			return Terms.Order.aggregation(getId(), "max", reverse);
		} else {
			throw new IllegalArgumentException("Invalid order: " + s);
		}
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		AggregationBuilder<?> aggregation = AggregationBuilders.terms(getId()).field(keyField).order(order).size(limit)
			.subAggregation(AggregationBuilders.stats(getId()).field(valueField));
		addAggregation(aggregation, builder);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		Terms terms = getAggregation(response);
		for (Terms.Bucket bucket : terms.getBuckets()) {
			Stats stats = bucket.getAggregations().get(getId());
			DateTime first = asDateTime(stats.getMin());
			if (first != null) {
				ObjectNode entryNode = result.addObject();
				LABEL.setValue(entryNode, bucket.getKey());
				COUNT.setValue(entryNode, bucket.getDocCount());
				FIRST.setValue(entryNode, first);
				LAST.setValue(entryNode, asDateTime(stats.getMax()));
			}
		}
		return result;
	}

	private DateTime asDateTime(double value) {
		return !Double.isInfinite(value) ? new DateTime((long) value, timezone) : null;
	}

	public static FacetBuilder builder(final FilterParser filterParser) {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				return new GanttFacet(
					options.get("id"),
					options.get("field"),
					options.get("key_field", String.class, Event.TIMESTAMP.getName()),
					options.get("order", String.class, "term"),
					options.get("limit", Integer.class, 10),
					options.get("timezone", DateTimeZone.class, DateTimeZone.UTC),
					filterParser.parse(options.get("filter")));
			}
		};
	}
}
