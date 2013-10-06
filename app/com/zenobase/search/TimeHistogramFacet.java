package com.zenobase.search;

import java.text.DateFormatSymbols;
import java.util.Locale;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class TimeHistogramFacet extends Facet {

	public static final String TYPE = "time_histogram";

	private final String field;
	private final Interval interval;

	private TimeHistogramFacet(String id, String field, Interval interval) {
		super(id);
		Preconditions.checkNotNull(field);
		Preconditions.checkNotNull(interval);
		this.field = field;
		this.interval = interval;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.termsFacet(getId())
			.field(interval.getField(field))
			.size(31).order(TermsFacet.ComparatorType.TERM));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		TermsFacet terms = response.getFacets().facet(TermsFacet.class, getId());
		Map<Integer, Integer> result = interval.emptyMap();
		for (TermsFacet.Entry entry : terms.getEntries()) {
			result.put(Integer.valueOf(entry.getTerm().toString()), entry.getCount());
		}
		return toJson(result);
	}

	private JsonNode toJson(Map<Integer, Integer> map) {
		ArrayNode node = Nodes.newArray();
		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
			ObjectNode entryNode = Nodes.newObject();
			entryNode.put("value", entry.getKey());
			entryNode.put("label", interval.getLabel(entry.getKey()));
			entryNode.put("count", entry.getValue());
			node.add(entryNode);
		}
		return node;
	}

	private enum Interval {

		HOUR_OF_DAY(0, 24) {
			@Override
			public String getLabel(int i) {
				return String.format("%02d", i);
			}
		},
		DAY_OF_WEEK(1, 7) {
			@Override
			public String getLabel(int i) {
				return format.getShortWeekdays()[i < 7 ? i + 1 : 1];
			}
		},
		DAY_OF_MONTH(1, 31) {
			@Override
			public String getLabel(int i) {
				return String.format("%02d", i);
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

		public Map<Integer, Integer> emptyMap() {
			Map<Integer, Integer> map = Maps.newTreeMap();
			for (int i = offset; i < offset + size; ++i) {
				map.put(i, 0);
			}
			return map;
		}

		public String getField(String parent) {
			return "$" + parent + "." + toString().toLowerCase();
		}

		public abstract String getLabel(int i);
	}

	public static FacetBuilder builder() {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				return new TimeHistogramFacet(
					options.get("id"),
					options.get("field", String.class, Event.TIMESTAMP.getName()),
					Interval.valueOf(options.get("interval").toUpperCase()));
			}
		};
	}
}
