package queries;

import models.Event;

import org.elasticsearch.action.search.SearchRequestBuilder;
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
import widgets.RatingWidget;
import widgets.TagWidget;
import widgets.TimelineWidget;
import widgets.Widget;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import common.Nodes;

public class BucketQuery {

	private static final QueryBuilder QUERY = QueryBuilders.matchAllQuery();
	private static final SortBuilder SORT = SortBuilders.fieldSort(Event.DATE_TIME.getName()).order(SortOrder.DESC);
	private static final ImmutableList<Widget> WIDGETS = ImmutableList.<Widget>of(
		new TagWidget(Event.TAG.getName(), 10), 
		new RatingWidget(Event.RATING.getName()),
		new TimelineWidget(Event.DATE_TIME.getName(), "month"));

	private final String bucketId;

	public BucketQuery(String bucketId) {
		this.bucketId = bucketId;
	}

	public BucketResult execute(IndexManager index) {
		SearchRequestBuilder request = index.prepareSearch(QUERY, SORT, 0, 10);
		for (Widget widget : WIDGETS) {
			widget.configure(request);
		}
		SearchResponse response = index.search(request);
		BucketResult result = new BucketResult(bucketId);
		for (SearchHit hit : response.hits()) {
			result.addEvent(new Event(hit.getId(), hit.getIndex(), Nodes.read(hit.source())));
		}
		for (Widget widget : WIDGETS) {
			result.addResult(widget.getClass().getSimpleName(), widget.getResult(response));
		}
		return result;
	}
}
