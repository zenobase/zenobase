package search;

import junit.framework.Assert;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

public class DateHistogramFacetTest {

	@Test
	public void test() {

		DateTimeZone timezone = DateTimeZone.forOffsetHours(-8);

		Node node = NodeBuilder.nodeBuilder().clusterName("test").local(true)
			.settings(ImmutableSettings.settingsBuilder().put("gateway.type", "none")).node();
		node.client().prepareIndex("test", "type", "1")
			.setSource("date", "2012-03-31T20:15:30-08:00").execute().actionGet();
		node.client().admin().indices().prepareRefresh().execute().actionGet();

		SearchResponse response = node.client().prepareSearch("test")
			.setQuery(QueryBuilders.matchAllQuery())
			.addFacet(FacetBuilders.dateHistogramFacet("years").field("date").interval("year").preZone(timezone.toString()))
			.addFacet(FacetBuilders.dateHistogramFacet("months").field("date").interval("month").preZone(timezone.toString()))
			.addFacet(FacetBuilders.dateHistogramFacet("days").field("date").interval("day").preZone(timezone.toString()))
			.addFacet(FacetBuilders.dateHistogramFacet("hours").field("date").interval("hour").preZone(timezone.toString()))
			.addFacet(FacetBuilders.dateHistogramFacet("minutes").field("date").interval("minute").preZone(timezone.toString()))
			.execute().actionGet();

		assertFacetDateTimeEquals(new DateTime(2012, 1, 1, 0, 0, timezone), response, "years", true);
		assertFacetDateTimeEquals(new DateTime(2012, 3, 1, 0, 0, timezone), response, "months", true);
		assertFacetDateTimeEquals(new DateTime(2012, 3, 31, 0, 0, timezone), response, "days", true);
		assertFacetDateTimeEquals(new DateTime(2012, 3, 31, 20, 0, timezone), response, "hours", false);
		assertFacetDateTimeEquals(new DateTime(2012, 3, 31, 20, 15, timezone), response, "minutes", false);

		node.close();
	}

	private void assertFacetDateTimeEquals(DateTime expected, SearchResponse response, String facetName, boolean correctOffset) {
		DateHistogramFacet facet = response.facets().facet(facetName);
		long time = facet.getEntries().get(0).time();
		if (correctOffset) {
			time = time - expected.getZone().getOffset(time);
		}
		Assert.assertEquals(facetName, expected, toDateTime(time, expected.getZone()));
	}

	private DateTime toDateTime(long time, DateTimeZone timezone) {
		return new DateTime(time, timezone);
	}
}
