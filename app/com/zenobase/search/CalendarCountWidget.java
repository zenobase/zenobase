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
import org.joda.time.LocalTime;
import org.joda.time.Minutes;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import com.zenobase.json.Nodes;

public class CalendarCountWidget extends Widget {

	public static final String TYPE = "calendar-count";

	private final DateFormatSymbols format = new DateFormatSymbols(Locale.US);
	private final String field;
	private final String interval; // hourOfDay, dayOfWeek or monthOfYear
	private final Minutes timezoneOffset;

	private CalendarCountWidget(String id, String field, String interval, Minutes timezoneOffset) {
		super(id);
		Preconditions.checkNotNull(field);
		Preconditions.checkNotNull(interval);
		this.field = field;
		this.interval = interval;
		this.timezoneOffset = timezoneOffset;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		String script = String.format("d=doc['%s'].date;d.addMinutes(%d);return d.%s", field, timezoneOffset.getMinutes(), interval) ;
		TermsFacetBuilder facet = FacetBuilders.termsFacet(getId()).scriptField(script).size(31);
		builder.facet(facet);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		TermsFacet terms = response.facets().facet(TermsFacet.class, getId());
		Map<Integer, Integer> result = newIntervalMap();
		for (TermsFacet.Entry entry : terms.entries()) {
			result.put(Integer.valueOf(entry.getTerm()), entry.getCount());
		}
		return toJson(result);
	}

	private Map<Integer, Integer> newIntervalMap() {
		if ("hourOfDay".equals(interval)) {
			return newIntervalMap(0, 24);
		}
		if ("dayOfWeek".equals(interval)) {
			return newIntervalMap(1, 7);
		}
		if ("monthOfYear".equals(interval)) {
			return newIntervalMap(1, 12);
		}
		throw new IllegalArgumentException("Invalid interval: " + interval);
	}

	private Map<Integer, Integer> newIntervalMap(int from, int size) {
		Map<Integer, Integer> map = Maps.newTreeMap();
		for (int i = from; i < from + size; ++i) {
			map.put(i, 0);
		}
		return map;
	}

	private JsonNode toJson(Map<Integer, Integer> map) {
		ArrayNode node = Nodes.newArray();
		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
			ObjectNode entryNode = Nodes.newObject();
			entryNode.put("label", getLabel(entry.getKey()));
			entryNode.put("count", entry.getValue());
			node.add(entryNode);
		}
		return node;
	}

	private String getLabel(int i) {
		if ("hourOfDay".equals(interval)) {
			return new LocalTime(i, 0).toString("HH:mm");
		}
		if ("dayOfWeek".equals(interval)) {
			return format.getShortWeekdays()[i < 7 ? i + 1 : 1];
		}
		if ("monthOfYear".equals(interval)) {
			return format.getShortMonths()[i - 1];
		}
		throw new IllegalArgumentException("Invalid interval: " + interval);
	}

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				return new CalendarCountWidget(
					options.get("id"),
					options.get("field"),
					options.get("interval"),
					Minutes.minutes(options.get("timezoneOffset", Integer.class, 0)));
			}
		};
	}
}
