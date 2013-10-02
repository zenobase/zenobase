package com.zenobase.search;

import java.math.BigDecimal;
import java.util.Map;

import javax.measure.unit.Unit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import com.zenobase.common.Measures;
import com.zenobase.json.MeasurementField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class ScatterPlotFacet extends Facet {

	public static final String TYPE = "scatterplot";

	private final String keyField;
	private final String xField, yField;
	private final FilterBuilder xFilter, yFilter;
	private final String interval;
	private final DateTimeZone timezone;
	private final Unit<?> xUnit, yUnit;
	private final Statistic statistic;

	public ScatterPlotFacet(String id, String keyField, String xField, Unit<?> xUnit, String xFilter, String yField, Unit<?> yUnit, String yFilter, String interval, DateTimeZone timezone, Statistic statistic) {
		super(id);
		this.keyField = keyField;
		this.xField = xField;
		this.xFilter = filter(xFilter);
		this.xUnit = xUnit;
		this.yField = yField;
		this.yUnit = yUnit;
		this.yFilter = filter(yFilter);
		this.interval = interval;
		this.timezone = timezone;
		this.statistic = statistic;
	}

	private static FilterBuilder filter(String expression) {
		return !Strings.isNullOrEmpty(expression)
			? FilterBuilders.queryFilter(QueryBuilders.queryString(expression))
			: FilterBuilders.matchAllFilter();
	}

	private String getXId() {
		return getId() + "-x";
	}

	private String getYId() {
		return getId() + "-y";
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		addFacet(builder, getXId(), xField, xUnit, xFilter);
		addFacet(builder, getYId(), yField, yUnit, yFilter);
	}

	private void addFacet(SearchSourceBuilder builder, String id, String field, Unit<?> unit, FilterBuilder filter) {
		builder.facet(FacetBuilders.dateHistogramFacet(id)
			.keyField(keyField).valueField(unit == Unit.ONE ? field : field + "." + MeasurementField.VALUE_SI.getName())
			.interval(interval)
			.preZone(timezone.toString())
			.preZoneAdjustLargeInterval(true)
			.facetFilter(filter));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		Map<Long, ObjectNode> values = Maps.newLinkedHashMap();
		process(response, values, getXId(), "x", xUnit);
		process(response, values, getYId(), "y", yUnit);
		return toJson(values.values());
	}

	private void process(SearchResponse response, Map<Long, ObjectNode> values, String id, String field, Unit<?> unit) {
		DateHistogramFacet facet = response.getFacets().facet(DateHistogramFacet.class, id);
		for (DateHistogramFacet.Entry entry : facet.getEntries()) {
			if (entry.getTotalCount() > 0) {
				ObjectNode entryNode = values.get(entry.getTime());
				if (entryNode == null) {
					entryNode = Nodes.newObject();
					values.put(entry.getTime(), entryNode);
				}
				entryNode.put(field, getValue(statistic.getValue(entry), unit));
			}
		}
	}

	private static BigDecimal getValue(double value, Unit<?> unit) {
		return unit == Unit.ONE ? Measures.round(value) : Measures.convert(value, unit);
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

	public static FacetBuilder builder() {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				String xUnit = options.get("unit_x");
				String yUnit = options.get("unit_y");
				String statistic = options.get("statistic", String.class, "avg");
				return new ScatterPlotFacet(
					options.get("id"),
					Event.TIMESTAMP.getName(),
					options.get("field_x"),
					xUnit != null ? Measures.parseUnit(xUnit) : Unit.ONE,
					options.get("filter_x"),
					options.get("field_y"),
					yUnit != null ? Measures.parseUnit(yUnit) : Unit.ONE,
					options.get("filter_y"),
					options.get("interval", String.class, "day"),
					options.get("timezone", DateTimeZone.class, DateTimeZone.UTC),
					Statistic.valueOf(statistic.toUpperCase()));
			}
		};
	}
}
