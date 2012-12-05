package com.zenobase.search;

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
		String script = String.format("d=doc['%s'].date;d.addMinutes(%d);d.%s", field, timezoneOffset.getMinutes(), interval) ;
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
		Map<Integer, Integer> map = Maps.newTreeMap();
		for (int i = 1; i <= getSize(); ++i) {
			map.put(i, 0);
		}
		return map;
	}

	private int getSize() {
		if ("hourOfDay".equals(interval)) {
			return 24;
		}
		if ("dayOfWeek".equals(interval)) {
			return 7;
		}
		if ("monthOfYear".equals(interval)) {
			return 12;
		}
		throw new IllegalArgumentException("Invalid interval: " + interval);
	}

	private JsonNode toJson(Map<Integer, Integer> map) {
		ArrayNode node = Nodes.newArray();
		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
			ObjectNode entryNode = Nodes.newObject();
			entryNode.put("label", entry.getKey());
			entryNode.put("count", entry.getValue());
			node.add(entryNode);
		}
		return node;
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
