package queries;

import models.Event;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.AbstractFacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.elasticsearch.search.facet.range.RangeFacet;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.YearMonth;

import services.IndexManager;
import services.NodeManager;

import com.google.common.collect.Lists;
import common.Nodes;

public class BucketQuery {

	private static final QueryBuilder QUERY = QueryBuilders.matchAllQuery();
	private static final SortBuilder SORT = SortBuilders.fieldSort(Event.DATE_TIME.getName()).order(SortOrder.DESC);
	private static final AbstractFacetBuilder FACET_TAG = FacetBuilders.termsFacet("tags").field(Event.TAG.getName()).size(10);
	private static final AbstractFacetBuilder FACET_RATING = FacetBuilders.rangeFacet("ratings").field(Event.RATING.getName())
		.addRange(0, 19).addRange(20, 39).addRange(40, 59).addRange(60, 79).addRange(80, 99).addUnboundedTo(100);
	private static final AbstractFacetBuilder FACET_MONTH = FacetBuilders.dateHistogramFacet("months").field(Event.DATE_TIME.getName()).interval("month");

	private final String bucketId;

	public BucketQuery(String bucketId) {
		this.bucketId = bucketId;
	}

	public BucketResult execute(IndexManager index) {
		SearchResponse response = index.search(QUERY, SORT, 0, 10, Lists.newArrayList(FACET_TAG, FACET_RATING, FACET_MONTH));
		BucketResult result = new BucketResult(bucketId);
		for (SearchHit hit : response.hits()) {
			result.addEvent(new Event(hit.getId(), hit.getIndex(), Nodes.read(hit.source())));
		}
		TermsFacet terms = response.facets().facet(TermsFacet.class, "tags");
		for (TermsFacet.Entry tag : terms.entries()) {
			result.addTag(tag.getTerm(), tag.getCount());
		}
		RangeFacet ratings = response.facets().facet(RangeFacet.class, "ratings");
		for (RangeFacet.Entry rating : ratings.entries()) {
			result.addRating(Integer.toString((int) rating.getFrom() / 20), (int) rating.getCount());
		}
		DateHistogramFacet months = response.facets().facet(DateHistogramFacet.class, "months");
		for (DateHistogramFacet.Entry month : months.entries()) {
			result.addMonth(new YearMonth(month.getTime()), (int) month.getCount());
		}
		return result;
	}
}
