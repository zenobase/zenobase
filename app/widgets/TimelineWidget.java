package widgets;

import models.Event;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.facet.AbstractFacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.joda.time.YearMonth;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

public class TimelineWidget implements Widget {

	private final AbstractFacetBuilder facet;

	public TimelineWidget(String field, String interval) {
		facet = FacetBuilders.dateHistogramFacet(getClass().getName()).field(Event.DATE_TIME.getName()).interval(interval);
	}

	@Override
	public void configure(SearchRequestBuilder request) {
		request.addFacet(facet);
	}

	@Override
	public Multiset<YearMonth> getResult(SearchResponse response) {
		Multiset<YearMonth> result = LinkedHashMultiset.create();
		DateHistogramFacet months = response.facets().facet(DateHistogramFacet.class, getClass().getName());
		for (DateHistogramFacet.Entry month : months.entries()) {
			result.add(new YearMonth(month.getTime()), (int) month.getCount());
		}
		return result;
	}
}
