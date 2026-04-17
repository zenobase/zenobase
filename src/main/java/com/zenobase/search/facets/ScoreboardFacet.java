package com.zenobase.search.facets;

import java.util.Collections;
import java.util.Objects;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.ExtendedStatsAggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Field;
import com.zenobase.json.Nodes;
import com.zenobase.search.constraints.FilterParser;

public class ScoreboardFacet extends FilteredFacet {

	public static final String TYPE = "scoreboard";

	private final String termField;
	private final String valueField;
	private final Unit<?> unit;
	private final String order;
	private final int limit;

	private ScoreboardFacet(
			String id,
			String termField,
			String valueField,
			Unit<?> unit,
			String order,
			int limit,
			@Nullable Query filter) {
		super(id, filter);
		this.termField = termField;
		this.valueField = valueField;
		this.unit = unit;
		this.order = order;
		this.limit = limit;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		String vf = getValueField();
		Aggregation terms = Aggregation.of(a -> a.terms(t -> {
					t.field(termField).size(limit);
					boolean asc = !order.startsWith("-");
					String orderField = asc ? order : order.substring(1);
					SortOrder sortOrder = asc ? SortOrder.Asc : SortOrder.Desc;
					switch (orderField) {
						case "count" -> t.order(Collections.singletonMap("_count", sortOrder));
						case "term" -> t.order(Collections.singletonMap("_key", sortOrder));
						default -> t.order(Collections.singletonMap(getId() + "." + orderField, sortOrder));
					}
					return t;
				})
				.aggregations(getId(), Aggregation.of(sa -> sa.extendedStats(e -> e.field(vf)))));
		addAggregation(getId(), terms, builder);
	}

	private String getValueField() {
		return Units.isDimensionless(unit)
				? valueField
				: Field.concat(valueField, DecimalMeasureField.VALUE_SI.getName());
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		ArrayNode result = Nodes.newArray();
		Aggregate aggregate = getAggregate(response);
		if (aggregate == null) {
			return result;
		}
		for (StringTermsBucket bucket : aggregate.sterms().buckets().array()) {
			Aggregate bucketAggregate = bucket.aggregations().get(getId());
			if (bucketAggregate == null) {
				continue;
			}
			ExtendedStatsAggregate stats = bucketAggregate.extendedStats();
			if (stats.count() > 0) {
				ObjectNode entryNode = result.addObject();
				entryNode.put("label", bucket.key());
				entryNode.put("count", bucket.docCount());
				addValue(entryNode, "min", stats.min());
				addValue(entryNode, "max", stats.max());
				addValue(entryNode, "sum", stats.sum());
				addValue(entryNode, "avg", stats.avg());
			}
		}
		return result;
	}

	private void addValue(ObjectNode parent, String property, @Nullable Double value) {
		if (value == null) {
			return;
		}
		if (!Units.isDimensionless(unit)) {
			ObjectNode node = parent.putObject(property);
			node.put("@value", Measures.convert(value, unit));
			node.put("unit", unit.toString());
		} else {
			parent.put(property, Measures.round(value));
		}
	}

	public static FacetBuilder builder(FilterParser filterParser) {
		return options -> {
			String unit = options.get("unit");
			return new ScoreboardFacet(
					Objects.requireNonNull(options.get("id")),
					Objects.requireNonNull(options.get("key_field")),
					Objects.requireNonNull(options.get("value_field")),
					unit != null ? Units.valueOf(unit) : Unit.ONE,
					Objects.requireNonNull(options.get("order", String.class, "-count")),
					Objects.requireNonNull(options.get("limit", Integer.class, 10)),
					filterParser.parse(options.get("filter")));
		};
	}
}
