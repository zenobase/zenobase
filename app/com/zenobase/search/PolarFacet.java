package com.zenobase.search;

import java.text.DateFormatSymbols;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import javax.measure.unit.Unit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.Field;
import com.zenobase.json.LocalDateTimeField;
import com.zenobase.json.MeasurementField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class PolarFacet extends Facet {

	public static final String TYPE = "polar";

	private final String keyField;
	private final String valueField;
	private final Interval interval;
	private final Unit<?> unit;
	private final FilterBuilder filter;

	private PolarFacet(String id, String keyField, String valueField, Interval interval, Unit<?> unit, FilterBuilder filter) {
		super(id);
		this.keyField = Preconditions.checkNotNull(keyField);
		this.valueField = Preconditions.checkNotNull(valueField);
		this.interval = Preconditions.checkNotNull(interval);
		this.unit = unit;
		this.filter = filter;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.termsStatsFacet(getId())
			.keyField(interval.getField(keyField))
			.valueField(unit == Unit.ONE ? valueField : Field.concat(valueField, MeasurementField.VALUE_SI.getName()))
			.order(TermsStatsFacet.ComparatorType.TERM).size(31)
			.facetFilter(filter));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		TermsStatsFacet terms = response.getFacets().facet(TermsStatsFacet.class, getId());
		Map<Integer, TermsStatsFacet.Entry> result = Collections.emptyMap();
		if (!terms.getEntries().isEmpty()) {
			result = interval.emptyMap();
			for (TermsStatsFacet.Entry entry : terms.getEntries()) {
				result.put(Integer.valueOf(entry.getTerm().toString()), entry);
			}
		}
		return toJson(result);
	}

	private JsonNode toJson(Map<Integer, TermsStatsFacet.Entry> map) {
		ArrayNode node = Nodes.newArray();
		for (Map.Entry<Integer, TermsStatsFacet.Entry> entry : map.entrySet()) {
			ObjectNode entryNode = Nodes.newObject();
			entryNode.put("value", entry.getKey());
			entryNode.put("label", interval.getLabel(entry.getKey()));
			if (entry.getValue() != null) {
				entryNode.put("count", entry.getValue().getCount());
				if (!keyField.equals(valueField) && entry.getValue().getTotalCount() > 0) {
					addValue(entryNode, "min", entry.getValue().getMin());
					addValue(entryNode, "max", entry.getValue().getMax());
					addValue(entryNode, "sum", entry.getValue().getTotal());
					addValue(entryNode, "avg", entry.getValue().getMean());
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

		private Interval(int offset, int size) {
			this.offset = offset;
			this.size = size;
		}

		public Map<Integer, TermsStatsFacet.Entry> emptyMap() {
			Map<Integer, TermsStatsFacet.Entry> map = Maps.newTreeMap();
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

	public static FacetBuilder builder(final FilterParser filterParser) {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				String unit = options.get("unit");
				return new PolarFacet(
					options.get("id"),
					options.get("key_field", String.class, Event.TIMESTAMP.getName()),
					options.get("value_field", String.class, Event.TIMESTAMP.getName()),
					Interval.valueOf(options.get("interval").toUpperCase()),
					unit != null ? Units.valueOf(unit) : Unit.ONE,
					filterParser.parse(options.get("filter")));
			}
		};
	}
}
