package com.zenobase.search;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.CalendarInterval;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramBucket;
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

public class ScatterPlotFacet extends Facet {

	public static final String TYPE = "scatterplot";

	private static final ImmutableMap<String, DurationFieldType> PERIODS =
			ImmutableMap.<String, DurationFieldType>builder()
					.put("year", DurationFieldType.years())
					.put("month", DurationFieldType.months())
					.put("week", DurationFieldType.weeks())
					.put("day", DurationFieldType.days())
					.put("hour", DurationFieldType.hours())
					.put("minute", DurationFieldType.minutes())
					.build();

	private final String keyField;
	private final Series x, y;
	private final String interval;
	private final @Nullable DateTimeZone timezone;
	private final int lag;

	public ScatterPlotFacet(
			String id, Series x, Series y, String keyField, String interval, @Nullable DateTimeZone timezone, int lag) {
		super(id);
		this.keyField = keyField;
		this.x = x;
		this.y = y;
		this.interval = interval;
		this.timezone = timezone;
		this.lag = lag;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		x.addAggregation(keyField, interval, timezone, builder);
		y.addAggregation(keyField, interval, timezone, builder);
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		Map<Long, ObjectNode> values = Maps.newLinkedHashMap();
		process(response, values, "x", x, 0);
		process(response, values, "y", y, lag);
		return toJson(values.values());
	}

	private void process(
			SearchResponse<ObjectNode> response, Map<Long, ObjectNode> values, String field, Series series, int lag) {
		Aggregate agg = Objects.requireNonNull(response.aggregations().get(series.id()));
		if (agg.isFilter()) {
			agg = Objects.requireNonNull(agg.filter().aggregations().get(series.id()));
		}
		for (DateHistogramBucket bucket : agg.dateHistogram().buckets().array()) {
			if (bucket.docCount() > 0) {
				long time = addLag(bucket.key(), lag);
				ObjectNode entryNode = values.get(time);
				if (entryNode == null) {
					entryNode = Nodes.newObject();
					values.put(time, entryNode);
				}
				entryNode.put(field, series.getValue(bucket));
			}
		}
	}

	private long addLag(long time, int lag) {
		return lag != 0
				? new DateTime(time, MoreObjects.firstNonNull(timezone, DateTimeZone.UTC))
						.plus(toPeriod(interval, lag))
						.getMillis()
				: time;
	}

	private static Period toPeriod(String interval, int value) {
		return new Period().withField(PERIODS.get(interval), value);
	}

	private JsonNode toJson(Iterable<ObjectNode> values) {
		ArrayNode node = Nodes.newArray();
		for (ObjectNode value : values) {
			JsonNode x = value.get("x");
			JsonNode y = value.get("y");
			if (x != null && !x.isNull() && y != null && !y.isNull()) {
				ArrayNode coords = Nodes.newArray();
				coords.add(x);
				coords.add(y);
				node.add(coords);
			}
		}
		return node;
	}

	private record Series(
			String id,
			String field,
			Unit<?> unit,
			Statistic statistic,
			@Nullable Query filter) {

		public void addAggregation(
				String keyField, String interval, @Nullable DateTimeZone timezone, SearchRequest.Builder builder) {
			String tz = MoreObjects.firstNonNull(timezone, DateTimeZone.UTC)
					.toTimeZone()
					.toZoneId()
					.getId();
			String dateField = timezone != null ? keyField : LocalDateTimeField.getLocalTimePath(keyField);
			String valueField =
					Units.isDimensionless(unit) ? field : Field.concat(field, DecimalMeasureField.VALUE_SI.getName());
			CalendarInterval calendarInterval = DateHistograms.parseInterval(interval);
			Aggregation dateHistogram = Aggregation.of(a -> a.dateHistogram(dh -> dh.field(dateField)
							.calendarInterval(calendarInterval)
							.timeZone(tz))
					.aggregations("stats", statistic.createAggregation(valueField))
					.aggregations("_count", Aggregation.of(vc -> vc.valueCount(v -> v.field(valueField)))));
			if (filter != null) {
				Aggregation filtered = Aggregation.of(a -> a.filter(filter).aggregations(id, dateHistogram));
				builder.aggregations(id, filtered);
			} else {
				builder.aggregations(id, dateHistogram);
			}
		}

		public @Nullable BigDecimal getValue(DateHistogramBucket bucket) {
			Aggregate aggregate = bucket.aggregations().get("_count");
			if (aggregate == null) {
				return null;
			}
			Double count = aggregate.valueCount().value();
			if (count == null || count.longValue() == 0) {
				return null;
			}
			double value = statistic.getValue(bucket);
			if (Double.isNaN(value) || Double.isInfinite(value)) {
				return null;
			}
			return Units.isDimensionless(unit) ? Measures.round(value) : Measures.convert(value, unit);
		}
	}

	enum Statistic {
		AVG {
			@Override
			Aggregation createAggregation(String field) {
				return Aggregation.of(a -> a.avg(v -> v.field(field)));
			}

			@Override
			double getValue(DateHistogramBucket bucket) {
				return unbox(getStatsAggregate(bucket).avg().value());
			}
		},
		MIN {
			@Override
			Aggregation createAggregation(String field) {
				return Aggregation.of(a -> a.min(v -> v.field(field)));
			}

			@Override
			double getValue(DateHistogramBucket bucket) {
				return unbox(getStatsAggregate(bucket).min().value());
			}
		},
		MAX {
			@Override
			Aggregation createAggregation(String field) {
				return Aggregation.of(a -> a.max(v -> v.field(field)));
			}

			@Override
			double getValue(DateHistogramBucket bucket) {
				return unbox(getStatsAggregate(bucket).max().value());
			}
		},
		SUM {
			@Override
			Aggregation createAggregation(String field) {
				return Aggregation.of(a -> a.sum(v -> v.field(field)));
			}

			@Override
			double getValue(DateHistogramBucket bucket) {
				return unbox(getStatsAggregate(bucket).sum().value());
			}
		},
		COUNT {
			@Override
			Aggregation createAggregation(String field) {
				return Aggregation.of(a -> a.valueCount(v -> v.field(field)));
			}

			@Override
			double getValue(DateHistogramBucket bucket) {
				return unbox(getStatsAggregate(bucket).valueCount().value());
			}
		};

		abstract Aggregation createAggregation(String field);

		abstract double getValue(DateHistogramBucket bucket);

		private static Aggregate getStatsAggregate(DateHistogramBucket bucket) {
			return Objects.requireNonNull(bucket.aggregations().get("stats"));
		}

		private static double unbox(@Nullable Double value) {
			return value != null ? value : 0.0;
		}
	}

	public static FacetBuilder builder(FilterParser filterParser) {
		return options -> {
			String id = Objects.requireNonNull(options.get("id"));
			Series x = new Series(
					id + "-x",
					Objects.requireNonNull(options.get("field_x")),
					parseUnit(options.get("unit_x")),
					parseStatistic(Objects.requireNonNull(options.get("statistic_x", String.class, "avg"))),
					filterParser.parse(options.get("filter_x")));
			Series y = new Series(
					id + "-y",
					Objects.requireNonNull(options.get("field_y")),
					parseUnit(options.get("unit_y")),
					parseStatistic(Objects.requireNonNull(options.get("statistic_y", String.class, "avg"))),
					filterParser.parse(options.get("filter_y")));
			return new ScatterPlotFacet(
					id,
					x,
					y,
					Objects.requireNonNull(options.get("key_field", String.class, Event.TIMESTAMP.getName())),
					Objects.requireNonNull(options.get("interval", String.class, "day")),
					options.get("timezone", DateTimeZone.class, null),
					Objects.requireNonNull(options.get("lag", Integer.class, 0)));
		};
	}

	private static Unit<?> parseUnit(@Nullable String value) {
		return value != null ? Units.valueOf(value) : Unit.ONE;
	}

	private static Statistic parseStatistic(String value) {
		return Statistic.valueOf(value.toUpperCase(Locale.ROOT));
	}
}
