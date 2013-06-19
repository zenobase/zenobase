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
import org.joda.time.DateTimeZone;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class TimeHistogramWidget extends Facet {

	public static final String TYPE = "time_histogram";

	private final String field;
	private final Interval interval;
	private final DateTimeZone timezone;

	private TimeHistogramWidget(String id, String field, Interval interval, DateTimeZone timezone) {
		super(id);
		Preconditions.checkNotNull(field);
		Preconditions.checkNotNull(interval);
		this.field = field;
		this.interval = interval;
		this.timezone = timezone;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		TermsFacetBuilder facet = FacetBuilders.termsFacet(getId()).size(31)
			.lang("js").scriptField(interval.script(field, timezone));
		builder.facet(facet);
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

		public String script(String field, DateTimeZone timezone) {
			return String.format("t=doc['%s'].date;t.addMillis(%d);v=t.%s;v", field, timezone.getOffset(0), method);
		}

		public abstract String getLabel(int i);
	}

	public static FacetBuilder builder() {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				return new TimeHistogramWidget(
					options.get("id"),
					options.get("field", String.class, Event.TIMESTAMP.getName()),
					Interval.valueOf(options.get("interval").toUpperCase()),
					options.get("timezone", DateTimeZone.class, DateTimeZone.UTC));
			}
		};
	}
}
