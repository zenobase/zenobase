package queries;

import java.io.OutputStream;
import java.io.PrintStream;

import models.Bucket;
import models.Event;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import services.IndexManager;
import widgets.RatingWidget;
import widgets.TagWidget;
import widgets.TimelineWidget;
import widgets.Widget;

import com.google.common.collect.ImmutableList;
import common.Nodes;

public class BucketQuery {

	private static final QueryBuilder QUERY = QueryBuilders.matchAllQuery();
	private static final SortBuilder SORT = SortBuilders.fieldSort(Event.DATE_TIME.getName()).order(SortOrder.DESC);
	private static final ImmutableList<Widget> WIDGETS = ImmutableList.<Widget>of(
		new TagWidget(Event.TAG.getName(), 10), 
		new RatingWidget(Event.RATING.getName()),
		new TimelineWidget(Event.DATE_TIME.getName(), "month"));

	private final Bucket bucket;

	public BucketQuery(Bucket bucket) {
		this.bucket = bucket;
	}

	public BucketResult execute(IndexManager index) {
		SearchResponse response = search(index);
		BucketResult result = new BucketResult(bucket);
		for (SearchHit hit : response.hits()) {
			result.addEvent(new Event(hit.getId(), hit.getIndex(), Nodes.read(hit.source())));
		}
		for (Widget widget : WIDGETS) {
			result.addFacet(widget.getClass().getSimpleName(), widget.getResult(response));
		}
		return result;
	}

	public SearchResponse search(IndexManager index) {
		SearchRequestBuilder request = index.prepareSearch(QUERY, SORT, 0, 10);
		for (Widget widget : WIDGETS) {
			widget.configure(request);
		}
		return index.search(request);
	}
}
