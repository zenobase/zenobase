package widgets;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.facet.AbstractFacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.range.RangeFacet;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

public class RatingWidget implements Widget {

	private final AbstractFacetBuilder facet;

	public RatingWidget(String field) {
		facet = FacetBuilders.rangeFacet(getClass().getName()).field(field)
			.addRange(0, 19).addRange(20, 39).addRange(40, 59)
			.addRange(60, 79).addRange(80, 99).addUnboundedTo(100);
	}

	public void configure(SearchRequestBuilder request) {
		request.addFacet(facet);
	}

	public Multiset<String> getResult(SearchResponse response) {
		Multiset<String> result = LinkedHashMultiset.create();
		RangeFacet ratings = response.facets().facet(RangeFacet.class, getClass().getName());
		for (RangeFacet.Entry rating : ratings.entries()) {
			result.add(Integer.toString((int) rating.getFrom() / 20), (int) rating.getCount());
		}
		return result;
	}
}
