package com.zenobase.search;

import java.text.DateFormatSymbols;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.LongTermsBucket;
import org.opensearch.client.opensearch._types.aggregations.StatsAggregate;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Field;
import com.zenobase.json.LocalDateTimeField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class PolarFacet extends FilteredFacet {

	public static final String TYPE = "polar";

	private final String keyField;
	private final String valueField;
	private final Interval interval;
	private final Unit<?> unit;

	private PolarFacet(String id, String keyField, String valueField, Interval interval, Unit<?> unit, Query filter) {
		super(id, filter);
		this.keyField = Preconditions.checkNotNull(keyField);
		this.valueField = Preconditions.checkNotNull(valueField);
		this.interval = Preconditions.checkNotNull(interval);
		this.unit = unit;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		String vf = unit == Unit.ONE ? valueField : Field.concat(valueField, DecimalMeasureField.VALUE_SI.getName());
		Aggregation terms = Aggregation.of(a -> a
			.terms(t -> t
				.field(interval.getField(keyField))
				.order(Collections.singletonMap("_key", SortOrder.Asc))
				.size(31)
			)
			.aggregations(getId(), Aggregation.of(sa -> sa.stats(s -> s.field(vf))))
		);
		addAggregation(getId(), terms, builder);
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		Aggregate agg = getAggregate(response);
		java.util.List<LongTermsBucket> buckets = agg.lterms().buckets().array();
		Map<Integer, StatsAggregate> result = Collections.emptyMap();
		if (!buckets.isEmpty()) {
			result = interval.emptyMap();
			for (LongTermsBucket bucket : buckets) {
				StatsAggregate stats = bucket.aggregations().get(getId()).stats();
				result.put(bucket.key().signed().intValue(), stats);
			}
		}
		return toJson(result);
	}

	private JsonNode toJson(Map<Integer, StatsAggregate> map) {
		ArrayNode node = Nodes.newArray();
		for (Map.Entry<Integer, StatsAggregate> entry : map.entrySet()) {
			ObjectNode entryNode = Nodes.newObject();
			entryNode.put("value", entry.getKey());
			entryNode.put("label", interval.getLabel(entry.getKey()));
			if (entry.getValue() != null) {
				entryNode.put("count", entry.getValue().count());
				if (!keyField.equals(valueField) && entry.getValue().count() > 0) {
					addValue(entryNode, "min", entry.getValue().min());
					addValue(entryNode, "max", entry.getValue().max());
					addValue(entryNode, "sum", entry.getValue().sum());
					addValue(entryNode, "avg", entry.getValue().avg());
				}
			} else {
				entryNode.put("count", 0);
			}
			node.add(entryNode);
		}
		return node;
	}

	private void addValue(ObjectNode parent, String property, double value) {
		if (unit != Unit.ONE) {
			ObjectNode node = parent.putObject(property);
			node.put("@value", Measures.convert(value, unit));
			node.put("unit", unit.toString());
		} else {
			parent.put(property, Measures.round(value));
		}
	}

	private enum Interval {

		HOUR_OF_DAY(0, 24) {
			@Override
			public String getLabel(int n) {
				return String.format("%dh", n);
			}
		},
		DAY_OF_WEEK(1, 7) {
			@Override
			public String getLabel(int n) {
				return format.getShortWeekdays()[n < 7 ? n + 1 : 1];
			}
		},
		DAY_OF_MONTH(1, 31) {
			@Override
			public String getLabel(int n) {
				return n + getDayOfMonthSuffix(n);
			}

			private String getDayOfMonthSuffix(int n) {
				Preconditions.checkArgument(n >= 1 && n <= 31, "Invalid day of month: %d", n);
				if (n >= 11 && n <= 13) {
					return "th";
				}
				switch (n % 10) {
					case 1: return "st";
					case 2: return "nd";
					case 3: return "rd";
					default: return "th";
				}
			}
		},
		MONTH_OF_YEAR(1, 12) {
			@Override
			public String getLabel(int i) {
				return format.getShortMonths()[i - 1];
			}
		};

		private static final DateFormatSymbols format = new DateFormatSymbols(Locale.US);

		private final int offset, size;

		Interval(int offset, int size) {
			this.offset = offset;
			this.size = size;
		}

		public Map<Integer, StatsAggregate> emptyMap() {
			Map<Integer, StatsAggregate> map = Maps.newTreeMap();
			for (int i = offset; i < offset + size; ++i) {
				map.put(i, null);
			}
			return map;
		}

		public String getField(String parent) {
			return LocalDateTimeField.getLocalTimePath(parent, toString().toLowerCase());
		}

		public abstract String getLabel(int i);
	}

	public static FacetBuilder builder(FilterParser filterParser) {
		return options -> {
			String unit = options.get("unit");
			return new PolarFacet(
				options.get("id"),
				options.get("key_field", String.class, Event.TIMESTAMP.getName()),
				options.get("value_field", String.class, Event.TIMESTAMP.getName()),
				Interval.valueOf(options.get("interval").toUpperCase()),
				unit != null ? Units.valueOf(unit) : Unit.ONE,
				filterParser.parse(options.get("filter")));
		};
	}
}
