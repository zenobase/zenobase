package search;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet;
import org.elasticsearch.search.facet.termsstats.TermsStatsFacet.ComparatorType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import schema.DateTimeType;
import schema.Type;

import common.Nodes;

public class GanttWidget implements Widget {

	private static final Type<DateTime> VALUE_TYPE = new DateTimeType();

	private final String id;
	private final String termField;
	private final String timeField;
	private final ComparatorType order;
	private final int limit;
	private final DateTimeZone timezone;

	private GanttWidget(String id, String termField, String timeField, ComparatorType order, int limit, DateTimeZone timezone) {
		this.id = id;
		this.termField = termField;
		this.timeField = timeField;
		this.order = order;
		this.limit = limit;
		this.timezone = timezone;
	}

	public String getId() {
		return id;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.termsStatsFacet(id)
			.keyField(termField).valueField(timeField).order(order).size(limit)); 
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		TermsStatsFacet terms = response.facets().facet(TermsStatsFacet.class, id);
		for (TermsStatsFacet.Entry entry : terms.entries()) {
			DateTime first = asDateTime(entry.getMin());
			if (first != null) {
				DateTime last = asDateTime(entry.getMax());
				ObjectNode entryNode = result.addObject();
				entryNode.put("label", entry.getTerm());
				entryNode.put("count", entry.getCount());
				VALUE_TYPE.setValue(entryNode, "first", first);
				VALUE_TYPE.setValue(entryNode, "last", last);
			}
		}
		return result;
	}

	private DateTime asDateTime(double value) {
		return !Double.isNaN(value) ? new DateTime((long) value, timezone) : null;
	}

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				return new GanttWidget(
					options.get("id"),
					options.get("termField"),
					options.get("timeField"),
					ComparatorType.valueOf(options.get("order", String.class, "term").toUpperCase()),
					options.get("limit", Integer.class, 10),
					options.get("timezone", DateTimeZone.class, DateTimeZone.UTC));
			}
		};
	}
}
