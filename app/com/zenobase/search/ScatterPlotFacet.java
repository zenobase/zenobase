package com.zenobase.search;

import java.math.BigDecimal;
import java.util.Map;

import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.CalendarInterval;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.DurationFieldType;
import org.joda.time.Period;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Field;
import com.zenobase.json.LocalDateTimeField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class ScatterPlotFacet extends Facet {

	public static final String TYPE = "scatterplot";

	private static final Map<String, DurationFieldType> PERIODS = ImmutableMap.<String, DurationFieldType>builder()
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
	private final DateTimeZone timezone;
	private final int lag;

	public ScatterPlotFacet(String id, Series x, Series y, String keyField, String interval, DateTimeZone timezone, int lag) {
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

	private void process(SearchResponse<ObjectNode> response, Map<Long, ObjectNode> values, String field, Series series, int lag) {
		Aggregate agg = response.aggregations().get(series.getId());
		if (agg != null && agg.isFilter()) {
			agg = agg.filter().aggregations().get(series.getId());
		}
		for (DateHistogramBucket bucket : agg.dateHistogram().buckets().array()) {
			if (bucket.docCount() > 0) {
				long time = addLag(DateHistograms.toEpochMillis(bucket.key()), lag);
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
		return lag != 0 ? new DateTime(time, Objects.firstNonNull(timezone, DateTimeZone.UTC)).plus(toPeriod(interval, lag)).getMillis() : time;
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

	private static class Series {

		private final String id;
		private final String field;
		private final Unit<?> unit;
		private final Statistic statistic;
		private final Query filter;

		public Series(String id, String field, Unit<?> unit, Statistic statistic, Query filter) {
			this.id = id;
			this.field = field;
			this.unit = unit;
			this.statistic = statistic;
			this.filter = filter;
		}

		public String getId() {
			return id;
		}

		public void addAggregation(String keyField, String interval, DateTimeZone timezone, SearchRequest.Builder builder) {
			String tz = Objects.firstNonNull(timezone, DateTimeZone.UTC).toTimeZone().toZoneId().getId();
			String dateField = timezone != null ? keyField : LocalDateTimeField.getLocalTimePath(keyField);
			String valueField = unit == Unit.ONE ? field : Field.concat(field, DecimalMeasureField.VALUE_SI.getName());
			CalendarInterval calendarInterval = DateHistograms.parseInterval(interval);
			Aggregation dateHistogram = Aggregation.of(a -> a
				.dateHistogram(dh -> dh
					.field(dateField)
					.calendarInterval(calendarInterval)
					.timeZone(tz)
				)
				.aggregations("stats", statistic.createAggregation(valueField))
				.aggregations("_count", Aggregation.of(vc -> vc.valueCount(v -> v.field(valueField))))
			);
			if (filter != null) {
				Aggregation filtered = Aggregation.of(a -> a
					.filter(filter)
					.aggregations(id, dateHistogram)
				);
				builder.aggregations(id, filtered);
			} else {
				builder.aggregations(id, dateHistogram);
			}
		}

		public BigDecimal getValue(DateHistogramBucket bucket) {
			long count = bucket.aggregations().get("_count").valueCount().value().longValue();
			if (count == 0) {
				return null;
			}
			double value = statistic.getValue(bucket);
			if (Double.isNaN(value) || Double.isInfinite(value)) {
				return null;
			}
			return unit == Unit.ONE ? Measures.round(value) : Measures.convert(value, unit);
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
				return bucket.aggregations().get("stats").avg().value();
			}
		},
		MIN {
			@Override
			Aggregation createAggregation(String field) {
				return Aggregation.of(a -> a.min(v -> v.field(field)));
			}
			@Override
			double getValue(DateHistogramBucket bucket) {
				return bucket.aggregations().get("stats").min().value();
			}
		},
		MAX {
			@Override
			Aggregation createAggregation(String field) {
				return Aggregation.of(a -> a.max(v -> v.field(field)));
			}
			@Override
			double getValue(DateHistogramBucket bucket) {
				return bucket.aggregations().get("stats").max().value();
			}
		},
		SUM {
			@Override
			Aggregation createAggregation(String field) {
				return Aggregation.of(a -> a.sum(v -> v.field(field)));
			}
			@Override
			double getValue(DateHistogramBucket bucket) {
				return bucket.aggregations().get("stats").sum().value();
			}
		},
		COUNT {
			@Override
			Aggregation createAggregation(String field) {
				return Aggregation.of(a -> a.valueCount(v -> v.field(field)));
			}
			@Override
			double getValue(DateHistogramBucket bucket) {
				return bucket.aggregations().get("stats").valueCount().value();
			}
		};

		abstract Aggregation createAggregation(String field);

		abstract double getValue(DateHistogramBucket bucket);
	}

	public static FacetBuilder builder(FilterParser filterParser) {
		return options -> {
			String id = options.get("id");
			Series x = new Series(id + "-x",
				options.get("field_x"),
				parseUnit(options.get("unit_x")),
				parseStatistic(options.get("statistic_x", String.class, "avg")),
				filterParser.parse(options.get("filter_x")));
			Series y = new Series(id + "-y",
				options.get("field_y"),
				parseUnit(options.get("unit_y")),
				parseStatistic(options.get("statistic_y", String.class, "avg")),
				filterParser.parse(options.get("filter_y")));
			return new ScatterPlotFacet(id, x, y,
				options.get("key_field", String.class, Event.TIMESTAMP.getName()),
				options.get("interval", String.class, "day"),
				options.get("timezone", DateTimeZone.class, null),
				options.get("lag", Integer.class, 0));
		};
	}

	private static Unit<?> parseUnit(String value) {
		return value != null ? Units.valueOf(value) : Unit.ONE;
	}

	private static Statistic parseStatistic(String value) {
		return Statistic.valueOf(value.toUpperCase());
	}
}
