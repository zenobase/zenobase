package com.zenobase.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet.ComparatorType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.json.DateTimeField;
import com.zenobase.json.LongField;
import com.zenobase.json.Nodes;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;

public class GanttFacet extends Facet {

	public static final String TYPE = "gantt";

	private static final TokenField LABEL = new TokenField("label");
	private static final LongField COUNT = new LongField("count");
	private static final DateTimeField FIRST = new DateTimeField("first");
	private static final DateTimeField LAST = new DateTimeField("last");

	private final String keyField;
	private final String valueField;
	private final ComparatorType order;
	private final int limit;
	private final DateTimeZone timezone;

	private GanttFacet(String id, String keyField, String valueField, ComparatorType order, int limit, DateTimeZone timezone) {
		super(id);
		this.keyField = keyField;
		this.valueField = valueField;
		this.order = order;
		this.limit = limit;
		this.timezone = timezone;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.termsStatsFacet(getId())
			.keyField(keyField).valueField(valueField).order(order).size(limit));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		TermsStatsFacet terms = response.getFacets().facet(TermsStatsFacet.class, getId());
		for (TermsStatsFacet.Entry entry : terms.getEntries()) {
			DateTime first = asDateTime(entry.getMin());
			if (first != null) {
				ObjectNode entryNode = result.addObject();
				LABEL.setValue(entryNode, entry.getTerm().toString());
				COUNT.setValue(entryNode, entry.getCount());
				FIRST.setValue(entryNode, first);
				LAST.setValue(entryNode, asDateTime(entry.getMax()));
			}
		}
		return result;
	}

	private DateTime asDateTime(double value) {
		return !Double.isInfinite(value) ? new DateTime((long) value, timezone) : null;
	}

	public static FacetBuilder builder() {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				return new GanttFacet(
					options.get("id"),
					options.get("field"),
					Event.TIMESTAMP.getName(),
					ComparatorType.valueOf(options.get("order", String.class, "term").toUpperCase()),
					options.get("limit", Integer.class, 10),
					options.get("timezone", DateTimeZone.class, DateTimeZone.UTC));
			}
		};
	}
}
