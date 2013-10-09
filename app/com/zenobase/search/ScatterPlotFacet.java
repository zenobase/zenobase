package com.zenobase.search;

import java.math.BigDecimal;
import java.util.Map;

import javax.measure.unit.Unit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacetBuilder;
import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

import com.zenobase.common.Measures;
import com.zenobase.json.MeasurementField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class ScatterPlotFacet extends Facet {

	public static final String TYPE = "scatterplot";

	private final String keyField;
	private final Series x, y;
	private final String interval;
	private final DateTimeZone timezone;

	public ScatterPlotFacet(String id, String keyField, Series x, Series y, String interval, DateTimeZone timezone) {
		super(id);
		this.keyField = keyField;
		this.x = x;
		this.y = y;
		this.interval = interval;
		this.timezone = timezone;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(x.createFacet(keyField, interval, timezone));
		builder.facet(y.createFacet(keyField, interval, timezone));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		Map<Long, ObjectNode> values = Maps.newLinkedHashMap();
		process(response, values, "x", x);
		process(response, values, "y", y);
		return toJson(values.values());
	}

	private void process(SearchResponse response, Map<Long, ObjectNode> values, String field, Series series) {
		DateHistogramFacet facet = response.getFacets().facet(DateHistogramFacet.class, series.getId());
		for (DateHistogramFacet.Entry entry : facet.getEntries()) {
			if (entry.getTotalCount() > 0) {
				ObjectNode entryNode = values.get(entry.getTime());
				if (entryNode == null) {
					entryNode = Nodes.newObject();
					values.put(entry.getTime(), entryNode);
				}
				entryNode.put(field, series.getValue(entry));
			}
		}
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

		public DateHistogramFacetBuilder createFacet(String keyField, String interval, DateTimeZone timezone) {
			return FacetBuilders.dateHistogramFacet(id)
				.keyField(keyField).valueField(unit == Unit.ONE ? field : field + "." + MeasurementField.VALUE_SI.getName())
				.interval(interval)
				.preZone(timezone.toString())
				.preZoneAdjustLargeInterval(true)
				.facetFilter(filter);
		}

		public BigDecimal getValue(DateHistogramFacet.Entry entry) {
			double value = statistic.getValue(entry);
			return unit == Unit.ONE ? Measures.round(value) : Measures.convert(value, unit);
		}
	}

	enum Statistic {
		AVG {
			@Override
			double getValue(DateHistogramFacet.Entry entry) {
				return entry.getMean();
			}
		},
		MIN {
			@Override
			double getValue(DateHistogramFacet.Entry entry) {
				return entry.getMin();
			}
		},
		MAX {
			@Override
			double getValue(DateHistogramFacet.Entry entry) {
				return entry.getMax();
			}
		},
		SUM {
			@Override
			double getValue(DateHistogramFacet.Entry entry) {
				return entry.getTotal();
			}
		},
		COUNT {
			@Override
			double getValue(DateHistogramFacet.Entry entry) {
				return entry.getCount();
			}
		};

		abstract double getValue(DateHistogramFacet.Entry entry);
	}

	public static FacetBuilder builder(final FilterBuilderSupport filterBuilder) {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				String id = options.get("id");
				Series x = new Series(id + "-x",
					options.get("field_x"),
					parseUnit(options.get("unit_x")),
					parseStatistic(options.get("statistic_x", String.class, "avg")),
					parseFilter(options.get("filter_x"), filterBuilder));
				Series y = new Series(id + "-y",
					options.get("field_y"),
					parseUnit(options.get("unit_y")),
					parseStatistic(options.get("statistic_y", String.class, "avg")),
					parseFilter(options.get("filter_y"), filterBuilder));
				return new ScatterPlotFacet(
					id, Event.TIMESTAMP.getName(), x, y,
					options.get("interval", String.class, "day"),
					options.get("timezone", DateTimeZone.class, DateTimeZone.UTC));
			}
		};
	}

	private static Unit<?> parseUnit(String value) {
		return value != null ? Measures.parseUnit(value) : Unit.ONE;
	}

	private static Statistic parseStatistic(String value) {
		return Statistic.valueOf(value.toUpperCase());
	}

	private static FilterBuilder parseFilter(String expressions, FilterBuilderSupport filterBuilder) {
		return filterBuilder.addConstraints(expressions).buildFilter();
	}
}
