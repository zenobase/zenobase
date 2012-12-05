package com.zenobase.search;

import java.text.DateFormatSymbols;
import java.util.Locale;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.joda.time.Minutes;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import com.zenobase.json.Nodes;

public class CalendarCountWidget extends Widget {

	public static final String TYPE = "calendar-count";

	private final String field;
	private final Interval interval;
	private final Minutes timezoneOffset;

	private CalendarCountWidget(String id, String field, Interval interval, Minutes timezoneOffset) {
		super(id);
		Preconditions.checkNotNull(field);
		Preconditions.checkNotNull(interval);
		this.field = field;
		this.interval = interval;
		this.timezoneOffset = timezoneOffset;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		TermsFacetBuilder facet = FacetBuilders.termsFacet(getId()).size(31)
			.lang("js").scriptField(interval.script(field, timezoneOffset));
		builder.facet(facet);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		TermsFacet terms = response.facets().facet(TermsFacet.class, getId());
		Map<Integer, Integer> result = interval.emptyMap();
		for (TermsFacet.Entry entry : terms.entries()) {
			result.put(Integer.valueOf(entry.getTerm()), entry.getCount());
		}
		return toJson(result);
	}

	private JsonNode toJson(Map<Integer, Integer> map) {
		ArrayNode node = Nodes.newArray();
		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
			ObjectNode entryNode = Nodes.newObject();
			entryNode.put("label", interval.getLabel(entry.getKey()));
			entryNode.put("count", entry.getValue());
			node.add(entryNode);
		}
		return node;
	}

	private enum Interval {

		HOUR_OF_DAY("getHourOfDay()", 0, 24) {
			@Override
			public String getLabel(int i) {
				return String.format("%02d", i);
			}
		},
		DAY_OF_WEEK("getDayOfWeek()", 1, 7) {
			@Override
			public String getLabel(int i) {
				return format.getShortWeekdays()[i < 7 ? i + 1 : 1];
			}
		},
		MONTH_OF_YEAR("getMonthOfYear()", 1, 12) {
			@Override
			public String getLabel(int i) {
				return format.getShortMonths()[i - 1];
			}
		};

		private static final DateFormatSymbols format = new DateFormatSymbols(Locale.US);

		private final String method;
		private final int offset, size;

		private Interval(String method, int offset, int size) {
			this.method = method;
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

		public String script(String field, Minutes timezoneOffset) {
			return String.format("t=doc['%s'].date;t.addMinutes(%d);v=t.%s;v", field, timezoneOffset.getMinutes(), method);
		}

		public abstract String getLabel(int i);
	}

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				return new CalendarCountWidget(
					options.get("id"),
					options.get("field"),
					Interval.valueOf(options.get("interval").toUpperCase()),
					Minutes.minutes(options.get("timezoneOffset", Integer.class, 0)));
			}
		};
	}
}
