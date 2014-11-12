package com.zenobase.search;

import java.math.BigDecimal;
import java.util.Map;

import javax.measure.unit.Unit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.metrics.ValuesSourceMetricsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.aggregations.metrics.min.Min;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Field;
import com.zenobase.json.LocalDateTimeField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class ScatterPlotFacet extends Facet {

	public static final String TYPE = "scatterplot";

	private static final Map<DateHistogram.Interval, DurationFieldType> PERIODS = ImmutableMap.<DateHistogram.Interval, DurationFieldType>builder()
		.put(DateHistogram.Interval.YEAR, DurationFieldType.years())
		.put(DateHistogram.Interval.MONTH, DurationFieldType.months())
		.put(DateHistogram.Interval.WEEK, DurationFieldType.weeks())
		.put(DateHistogram.Interval.DAY, DurationFieldType.days())
		.put(DateHistogram.Interval.HOUR, DurationFieldType.hours())
		.put(DateHistogram.Interval.MINUTE, DurationFieldType.minutes())
		.build();

	private final String keyField;
	private final Series x, y;
	private final DateHistogram.Interval interval;
	private final DateTimeZone timezone;
	private final int lag;

	public ScatterPlotFacet(String id, Series x, Series y, String keyField, String interval, DateTimeZone timezone, int lag) {
		super(id);
		this.keyField = keyField;
		this.x = x;
		this.y = y;
		this.interval = DateHistograms.parseInterval(interval);
		this.timezone = timezone;
		this.lag = lag;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.aggregation(x.createAggregation(keyField, interval, timezone));
		builder.aggregation(y.createAggregation(keyField, interval, timezone));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		Map<Long, ObjectNode> values = Maps.newLinkedHashMap();
		process(response, values, "x", x, 0);
		process(response, values, "y", y, lag);
		return toJson(values.values());
	}

	private void process(SearchResponse response, Map<Long, ObjectNode> values, String field, Series series, int lag) {
		DateHistogram histogram = getAggregation(response, series.getId());
		for (DateHistogram.Bucket bucket : histogram.getBuckets()) {
			if (bucket.getDocCount() > 0) {
				long time = addLag(bucket.getKeyAsNumber().longValue(), lag);
				ObjectNode entryNode = values.get(time);
				if (entryNode == null) {
					entryNode = Nodes.newObject();
					values.put(time, entryNode);
				}
				entryNode.put(field, series.getValue(bucket));
			}
		}
	}

	private <A extends Aggregation> A getAggregation(SearchResponse response, String id) {
		A aggregation = response.getAggregations().get(id);
		if (aggregation instanceof HasAggregations) {
			aggregation = ((HasAggregations) aggregation).getAggregations().get(id);
		}
		return aggregation;
	}

	private long addLag(long time, int lag) {
		return lag != 0 ? new DateTime(time, Objects.firstNonNull(timezone, DateTimeZone.UTC)).plus(toPeriod(interval, lag)).getMillis() : time;
	}

	private static Period toPeriod(DateHistogram.Interval interval, int value) {
		return new Period().withField(PERIODS.get(interval), value);
	}

	private JsonNode toJson(Iterable<ObjectNode> values) {
		ArrayNode node = Nodes.newArray();
		for (ObjectNode value : values) {
			JsonNode x = value.get("x");
			JsonNode y = value.get("y");
			if (x != null && y != null) {
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
		private final FilterBuilder filter;

		public Series(String id, String field, Unit<?> unit, Statistic statistic, FilterBuilder filter) {
			this.id = id;
			this.field = field;
			this.unit = unit;
			this.statistic = statistic;
			this.filter = filter;
		}

		public String getId() {
			return id;
		}

		public AggregationBuilder<?> createAggregation(String keyField, DateHistogram.Interval interval, DateTimeZone timezone) {
			AggregationBuilder<?> aggregation = AggregationBuilders.dateHistogram(id)
				.field(timezone != null ? keyField : LocalDateTimeField.getLocalTimePath(keyField))
				.interval(interval)
				.preZone(Objects.firstNonNull(timezone, DateTimeZone.UTC).toString())
				.preZoneAdjustLargeInterval(true)
				.subAggregation(statistic.createAggregation().field(getField()));
			if (filter != null) {
				aggregation = AggregationBuilders.filter(id).filter(filter).subAggregation(aggregation);
			}
			return aggregation;
		}

		private String getField() {
			return unit == Unit.ONE ? field : Field.concat(field, DecimalMeasureField.VALUE_SI.getName());
		}

		public BigDecimal getValue(DateHistogram.Bucket bucket) {

			double value = statistic.getValue(bucket);
			return unit == Unit.ONE ? Measures.round(value) : Measures.convert(value, unit);
		}
	}

	enum Statistic {
		AVG {
			@Override
			ValuesSourceMetricsAggregationBuilder<?> createAggregation() {
				return AggregationBuilders.avg(ID);
			}
			@Override
			double getValue(DateHistogram.Bucket bucket) {
				Avg aggregation = bucket.getAggregations().get(ID);
				return aggregation.getValue();
			}
		},
		MIN {
			@Override
			ValuesSourceMetricsAggregationBuilder<?> createAggregation() {
				return AggregationBuilders.min(ID);
			}
			@Override
			double getValue(DateHistogram.Bucket bucket) {
				Min aggregation = bucket.getAggregations().get(ID);
				return aggregation.getValue();
			}
		},
		MAX {
			@Override
			ValuesSourceMetricsAggregationBuilder<?> createAggregation() {
				return AggregationBuilders.max(ID);
			}
			@Override
			double getValue(DateHistogram.Bucket bucket) {
				Max aggregation = bucket.getAggregations().get(ID);
				return aggregation.getValue();
			}
		},
		SUM {
			@Override
			ValuesSourceMetricsAggregationBuilder<?> createAggregation() {
				return AggregationBuilders.sum(ID);
			}
			@Override
			double getValue(DateHistogram.Bucket bucket) {
				Sum aggregation = bucket.getAggregations().get(ID);
				return aggregation.getValue();
			}
		},
		COUNT {
			@Override
			ValuesSourceMetricsAggregationBuilder<?> createAggregation() {
				return AggregationBuilders.count(ID);
			}
			@Override
			double getValue(DateHistogram.Bucket bucket) {
				Max aggregation = bucket.getAggregations().get(ID);
				return aggregation.getValue();
			}
		};

		private static final String ID = "stats";

		abstract ValuesSourceMetricsAggregationBuilder<?> createAggregation();

		abstract double getValue(DateHistogram.Bucket bucket);
	}

	public static FacetBuilder builder(final FilterParser filterParser) {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
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
			}
		};
	}

	private static Unit<?> parseUnit(String value) {
		return value != null ? Units.valueOf(value) : Unit.ONE;
	}

	private static Statistic parseStatistic(String value) {
		return Statistic.valueOf(value.toUpperCase());
	}
}
