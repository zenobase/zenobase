package com.zenobase.search;

import java.util.Map;

import junit.framework.Assert;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import com.google.common.collect.ImmutableMap;

import com.zenobase.services.ElasticSearchTestSupport;

public class DateHistogramFacetTest extends ElasticSearchTestSupport {

	private final DateTimeZone timezone = DateTimeZone.forOffsetHours(-8);
	private final String index = "test";
	private final String type = "type";
	private final String field = "date";

	@Test
	public void test() {

		String value = "2012-03-31T20:15:30-08:00";

		index(value);

		ImmutableMap<String, DateTime> intervals = ImmutableMap.<String, DateTime>builder()
			.put("year", new DateTime(2012, 1, 1, 0, 0, timezone))
			.put("month", new DateTime(2012, 3, 1, 0, 0, timezone))
			.put("day", new DateTime(2012, 3, 31, 0, 0, timezone))
			.put("hour", new DateTime(2012, 3, 31, 20, 0, timezone))
			.put("minute", new DateTime(2012, 3, 31, 20, 15, timezone))
			.build();

		SearchResponse response = search(intervals);
		for (Map.Entry<String, DateTime> entry : intervals.entrySet()) {
			assertFacet(entry.getValue(), response, entry.getKey());
		}
	}

	private void index(String value) {
		getClient().prepareIndex(index, type).setSource(field, value).execute().actionGet();
		getClient().admin().indices().prepareRefresh().execute().actionGet();
	}

	private SearchResponse search(ImmutableMap<String, DateTime> intervals) {
		SearchRequestBuilder request = getClient().prepareSearch(index).setQuery(QueryBuilders.matchAllQuery());
		for (String interval : intervals.keySet()) {
			request.addFacet(FacetBuilders.dateHistogramFacet(interval).field(field)
				.interval(interval).preZone(timezone.toString()).preZoneAdjustLargeInterval(true));
		}
		return request.execute().actionGet();
	}

	private static void assertFacet(DateTime expected, SearchResponse response, String interval) {
		DateHistogramFacet facet = response.facets().facet(interval);
		long time = facet.getEntries().get(0).time();
		Assert.assertEquals(interval, expected, new DateTime(time, expected.getZone()));
	}
}
