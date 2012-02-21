package search;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import common.Nodes;

public class TimelineWidget implements Widget {

	private final String id;
	private final String field;
	private final String interval;

	public TimelineWidget(String id, String field, String interval) {
		this.id = id;
		this.field = field;
		this.interval = interval;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(FacetBuilders.dateHistogramFacet(id)
			.field(field).interval(interval).preZone("-08:00"));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		DateHistogramFacet months = response.facets().facet(DateHistogramFacet.class, id);
		for (DateHistogramFacet.Entry entry : months.entries()) {
			ObjectNode entryNode = result.addObject();
			entryNode.put("label", getLabel(entry));
			entryNode.put("count", entry.getCount());
		}
		return result;
	}

	private String getLabel(DateHistogramFacet.Entry entry) {
		DateTime time = new DateTime(entry.getTime(), DateTimeZone.UTC);
		if ("year".equals(interval)) {
			return Integer.toString(new YearMonth(time).getYear());
		}
		if ("month".equals(interval)) {
			return new YearMonth(time).toString();
		}
		if ("day".equals(interval)) {
			return new LocalDate(time).toString();
		}
		throw new AssertionError("Can't handle interval: " + interval);
	}

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				return new TimelineWidget(
					options.get("id"),
					options.get("field"),
					options.get("interval"));
			}
		};
	}
}
