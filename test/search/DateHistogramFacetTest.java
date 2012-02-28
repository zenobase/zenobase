package search;

import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

public class DateHistogramFacetTest {

	@Test
    public void test() {
        Node node = NodeBuilder.nodeBuilder().clusterName("test").local(true)
                .settings(ImmutableSettings.settingsBuilder().put("gateway.type", "none"))
                .node();

        node.client().prepareIndex("test", "type", "1")
                .setSource("date", "2012-01-15T01:00:00-08:00")
                .execute().actionGet();
        node.client().admin().indices().prepareRefresh().execute().actionGet();

        SearchResponse searchResponse = node.client().prepareSearch("test")
                .setQuery(QueryBuilders.matchAllQuery())
                        // facet1 will take the offset into account, but return UTC
                .addFacet(FacetBuilders.dateHistogramFacet("facet1").field("date").interval("month").preZone("-08:00"))
                        // facet 2 will take offset into account, but also return it with an offset
                .addFacet(FacetBuilders.dateHistogramFacet("facet2").field("date").interval("month").preZone("-08:00").postZone("-08:00"))
                .execute().actionGet();

        DateHistogramFacet facet = searchResponse.facets().facet("facet1");
        long utcExpected = ISODateTimeFormat.dateOptionalTimeParser().withZone(DateTimeZone.UTC).parseMillis("2012-01-01T00:00:00");
        // long pstExpected = ISODateTimeFormat.dateOptionalTimeParser().withZone(DateTimeZone.forOffsetHours(-8)).parseMillis("2012-01-01T00:00:00");
        System.out.println("UTC: Expected: " + utcExpected + ", got: " + facet.getEntries().get(0).time());
        facet = searchResponse.facets().facet("facet2");
        long offsetExpected = utcExpected - TimeUnit.HOURS.toMillis(-8);
        System.out.println("Offset: Expected: " + offsetExpected + ", got: " + facet.getEntries().get(0).time());
        System.out.println(new DateTime(offsetExpected, DateTimeZone.forOffsetHours(-8)));

        node.close();
    }
}