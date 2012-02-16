package queries;

import java.util.Set;

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
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import common.Nodes;

public class BucketQuery {

	private static final QueryBuilder QUERY = QueryBuilders.matchAllQuery();
	private static final SortBuilder SORT = SortBuilders.fieldSort(Event.DATE_TIME.getName()).order(SortOrder.DESC);
	private static final ImmutableList<Widget> WIDGETS = ImmutableList.<Widget>of(
		new TagWidget(Event.TAG.getName(), 10), 
		new RatingWidget(Event.RATING.getName()),
		new TimelineWidget(Event.DATE_TIME.getName(), "month"));

	private final Bucket bucket;
	private int offset = 0;
	private int limit = 10;
	private final Set<Widget> widgets = Sets.newLinkedHashSet();

	public BucketQuery(Bucket bucket) {
		this.bucket = bucket;
	}

	public BucketQuery setOffset(int offset) {
		this.offset = offset;
		return this;
	}

	public BucketQuery setLimit(int limit) {
		this.limit = limit;
		return this;
	}

	public BucketQuery addFacets(String[] facets) {
		for (String facet : facets) {
			addFacet(facet);
		}
		return this;
	}

	public BucketQuery addFacet(String facet) {
		for (Widget widget : WIDGETS) {
			if (widget.getClass().getSimpleName().equals(facet)) {
				widgets.add(widget);
			}
		}
		return this;
	}

	public BucketResult execute(IndexManager index) {
		SearchResponse response = search(index);
		BucketResult result = new BucketResult(bucket);
		result.setTotal(Ints.checkedCast(response.hits().getTotalHits()));
		for (SearchHit hit : response.hits()) {
			result.addEvent(new Event(hit.getId(), hit.getIndex(), Nodes.read(hit.source())));
		}
		for (Widget widget : widgets) {
			result.addFacet(widget.getClass().getSimpleName(), widget.getResult(response));
		}
		return result;
	}

	public SearchResponse search(IndexManager index) {
		SearchRequestBuilder request = index.prepareSearch(QUERY, SORT, offset, limit);
		for (Widget widget : WIDGETS) {
			widget.configure(request);
		}
		return index.search(request);
	}
}
