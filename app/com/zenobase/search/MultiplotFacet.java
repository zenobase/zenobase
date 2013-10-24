package com.zenobase.search;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.measure.unit.Unit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.zenobase.common.Measures;
import com.zenobase.common.OffsetIntervals;
import com.zenobase.json.Field;
import com.zenobase.json.MeasurementField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class MultiplotFacet extends Facet {

	public static final String TYPE = "multiplot";

	private final String keyField;
	private final List<String> fields;
	private final List<String> units;
	private final String interval;
	private final DateTimeZone timezone;
	private final Statistic statistic;

	public MultiplotFacet(String id, String keyField, Iterable<String> fields, Iterable<String> units, String interval, DateTimeZone timezone, Statistic statistic) {
		super(id);
		this.keyField = keyField;
		this.fields = Lists.newArrayList(fields);
		this.units = Lists.newArrayList(units);
		this.interval = interval;
		this.timezone = timezone;
		this.statistic = statistic;
		Preconditions.checkArgument(this.fields.size() == this.units.size());
	}

	private String getId(int series) {
		return getId() + "-" + series;
	}

	private Unit<?> getUnit(int series) {
		return Measures.parseUnit(units.get(series));
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		for (int i = 0; i < fields.size(); ++i) {
			addFacet(builder, getId(i), fields.get(i), getUnit(i));
		}
	}

	private void addFacet(SearchSourceBuilder builder, String id, String field, Unit<?> unit) {
		builder.facet(FacetBuilders.dateHistogramFacet(id)
			.keyField(keyField).valueField(unit == Unit.ONE ? field : Field.concat(field, MeasurementField.VALUE_SI.getName()))
			.interval(interval)
			.preZone(timezone.toString())
			.preZoneAdjustLargeInterval(true));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		Map<String, ObjectNode> values = Maps.newLinkedHashMap();
		for (int i = 0; i < fields.size(); ++i) {
			process(response, values, getId(i), fields.get(i), getUnit(i));
		}
		return toJson(values.values());
	}

	private void process(SearchResponse response, Map<String, ObjectNode> values, String id, String field, Unit<?> unit) {
		DateHistogramFacet facet = response.getFacets().facet(DateHistogramFacet.class, id);
		for (DateHistogramFacet.Entry entry : facet.getEntries()) {
			if (entry.getTotalCount() > 0) {
				String key = getLabel(toDateTime(entry.getTime()));
				ObjectNode entryNode = values.get(key);
				if (entryNode == null) {
					entryNode = Nodes.newObject();
					entryNode.put("label", key);
					entryNode.put("time", entry.getTime() + timezone.getOffset(entry.getTime()));
					values.put(key, entryNode);
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
			node.add(value);
		}
		return node;
	}

	private DateTime toDateTime(long time) {
		return new DateTime(time, timezone);
	}

	private String getLabel(DateTime time) {
		return OffsetIntervals.toString(time, interval);
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
				return new MultiplotFacet(
					options.get("id"),
					Event.TIMESTAMP.getName(),
					Splitter.on('|').split(options.get("fields", String.class, "")),
					Splitter.on('|').split(options.get("units", String.class, "")),
					options.get("interval", String.class, "month"),
					options.get("timezone", DateTimeZone.class, DateTimeZone.UTC),
					Statistic.valueOf(options.get("statistic", String.class, "avg").toUpperCase()));
			}
		};
	}
}
